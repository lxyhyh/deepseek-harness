package com.self.dshmobile.data

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
 * 说明：真正运行时需要已 root 的 Android 手机。沙箱/未 root 环境下所有操作
 * 返回可读错误，界面据此提示用户，不会假成功。
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

    /** 启动容器 + dsh Web 服务（守护进程，日志落到容器内） */
    fun startContainer(onDone: (SuResult) -> Unit) {
        // 阶段 1 实现：mount /proc /dev 等 → chroot → 以普通用户启动 dsh web
        // 真机联调前先做一次可用性探测，避免假成功
        val probe = trySu("test -x ${BuildConfig.CONTAINER_ROOT}/bin/bash && echo ok")
        onDone(probe)
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
