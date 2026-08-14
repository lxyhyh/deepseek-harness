package com.self.dshmobile.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.self.dshmobile.BuildConfig

/** 执行结果：成功 / 失败（含原因）/ 无 root */
sealed class SuResult {
    data class Ok(val output: String) : SuResult()
    data class Fail(val message: String) : SuResult()
    object NoRoot : SuResult()
}

/**
 * 容器管理：通过通用 su 协议（KernelSU / Magisk 等框架）操作 chroot 容器。
 *
 * 容器 rootfs 以内置归档（APK assets，见 [BuildConfig.CONTAINER_ASSET]）随 App 打包，
 * 首次启动用 [deployFromAssets] 解压到 [BuildConfig.CONTAINER_ROOT]，无需联网下载。
 * 沙箱/未 root 环境下所有操作返回可读错误，界面据此提示用户，不会假成功。
 */
object ContainerManager {

    private fun trySu(command: String): SuResult = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code == 0) SuResult.Ok(out) else SuResult.Fail(out.trim().ifEmpty { "命令退出码 $code" })
    } catch (e: Exception) {
        SuResult.NoRoot
    }

    /** 当前设备是否具备 root（uid=0） */
    fun hasRoot(): Boolean {
        val r = trySu("id")
        return r is SuResult.Ok && r.output.contains("uid=0")
    }

    /** 容器 rootfs 是否已部署（/bin 与 /usr/bin 存在） */
    fun isContainerDeployed(): Boolean =
        trySu("test -d ${BuildConfig.CONTAINER_ROOT}/bin && test -d ${BuildConfig.CONTAINER_ROOT}/usr && echo yes") is SuResult.Ok

    /** 容器是否正在运行（以 `dsh web` 端口 3080 是否可连为准） */
    fun isContainerRunning(): Boolean {
        val r = trySu("pgrep -f 'dsh.*web' >/dev/null && echo yes")
        return r is SuResult.Ok
    }

    /**
     * 从 APK 内置归档部署容器：内置容器以分块形式打进 assets（
     * [BuildConfig.CONTAINER_ASSET] 加 `.00/.01/…` 后缀），每块经 `su` 拼接到
     * 临时文件后统一解压到 [BuildConfig.CONTAINER_ROOT]。全程无需下载、无中间文件。
     *
     * 在后台线程执行，进度与结果回主线程。
     * @param context - 用于读取 assets（取 app context）。
     * @param onProgress - 解压进度回调（0..100）。
     * @param onDone - 结束回调（主线程）。
     */
    fun deployFromAssets(context: Context, onProgress: (Int) -> Unit, onDone: (SuResult) -> Unit) {
        Thread {
            val main = Handler(Looper.getMainLooper())
            val post = { r: SuResult -> main.post { onDone(r) } }
            try {
                // 1) 清理旧容器，保证目录干净
                val clean = trySu("rm -rf ${BuildConfig.CONTAINER_ROOT} && mkdir -p ${BuildConfig.CONTAINER_ROOT}")
                if (clean !is SuResult.Ok) {
                    post(clean)
                    return@Thread
                }
                // 2) 逐块读取并拼接（块大小远小于构建内存上限，避免大文件二次压缩/打包时 OOM）
                val tmp = "${BuildConfig.CONTAINER_ROOT}/.deploy.tar.gz"
                var total = 0L
                val totalBytes = (0 until PART_COUNT).sumOf { i ->
                    context.assets.open(partName(i)).use { it.available().toLong() }
                }
                for (i in 0 until PART_COUNT) {
                    val proc = ProcessBuilder("su", "-c", "cat >> $tmp").redirectErrorStream(true).start()
                    val drain = Thread {
                        proc.inputStream.bufferedReader().use { it.readLines() }
                    }
                    drain.start()
                    context.assets.open(partName(i)).use { input ->
                        val output = proc.outputStream
                        val buf = ByteArray(256 * 1024)
                        var last = 0
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            total += n
                            val pct = (total * 100 / totalBytes).toInt()
                            if (pct - last >= 1) {
                                last = pct
                                val p = pct
                                main.post { onProgress(p) }
                            }
                        }
                        output.flush()
                        output.close()
                    }
                    drain.join()
                    if (proc.waitFor() != 0) {
                        post(SuResult.Fail("分块 ${partName(i)} 写入失败（退出码 ${proc.exitValue()}）"))
                        return@Thread
                    }
                }
                // 3) 解压 + 清理临时归档
                val extract = trySu("tar xzf $tmp -C ${BuildConfig.CONTAINER_ROOT} && rm -f $tmp")
                post(if (extract is SuResult.Ok) SuResult.Ok("内置容器部署完成") else extract)
            } catch (e: Exception) {
                post(SuResult.Fail(e.message ?: e.toString()))
            }
        }.start()
    }

    /** 内置容器归档分块数（与 assets 内 `*.tar.gz.00/01/…` 一致） */
    private const val PART_COUNT = 4

    /** 第 [i] 个归档分块的 assets 名 */
    private fun partName(i: Int): String = "${BuildConfig.CONTAINER_ASSET}.${String.format("%02d", i)}"

    /** 启动容器：挂载 proc/sys/dev → chroot → 后台拉起 dsh Web 服务 */
    fun startContainer(onDone: (SuResult) -> Unit) {
        val script = """
            set -e
            CT=${BuildConfig.CONTAINER_ROOT}
            test -x ${'$'}CT/bin/bash
            mkdir -p ${'$'}CT/proc ${'$'}CT/dev ${'$'}CT/sys ${'$'}CT/tmp
            mount -t proc proc ${'$'}CT/proc 2>/dev/null || true
            mount -t sysfs sysfs ${'$'}CT/sys 2>/dev/null || true
            mount --rbind /dev ${'$'}CT/dev 2>/dev/null || true
            mount --rbind /dev/pts ${'$'}CT/dev/pts 2>/dev/null || true
            setsid nohup chroot ${'$'}CT /opt/dsh/bin/dsh web --host 127.0.0.1 --port ${BuildConfig.DSH_WEB_PORT} >/dev/null 2>&1 &
            echo started
        """.trimIndent()
        onDone(trySu(script))
    }

    /** 停止容器（杀掉 dsh web 进程） */
    fun stopContainer(): SuResult = trySu("pkill -f 'dsh.*web' || true")

    /** 重启容器 */
    fun restartContainer(onDone: (SuResult) -> Unit) {
        stopContainer()
        startContainer(onDone)
    }

    /** 复制宿主文件到容器工作区（供文件发送：系统选择器选中后复制进容器） */
    fun copyIntoWorkspace(hostPath: String, targetDir: String = "/root/inbox"): SuResult =
        trySu("mkdir -p ${BuildConfig.CONTAINER_ROOT}$targetDir && cp '$hostPath' ${BuildConfig.CONTAINER_ROOT}$targetDir/")

    /** 备份打包：stop → tar 打包容器+配置+会话 → Download。勾选内容由参数决定 */
    fun backup(snapshot: String): SuResult =
        trySu("tar czf /sdcard/Download/dsh-backup-${System.currentTimeMillis()}.tar.gz -C / ${BuildConfig.CONTAINER_ROOT.trimStart('/')}")
}
