/* dsh Mobile 原型 · 交互逻辑（vanilla JS，纯前端 mock） */
(function () {
  "use strict";

  /* ---------- Lucide 内联图标（离线可用，零依赖） ---------- */
  var ICONS = {
    menu: '<line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="18" x2="20" y2="18"/>',
    x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
    plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
    mic: '<path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="22"/>',
    send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
    "arrow-left": '<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
    package: '<path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"/><path d="M12 22V12"/><path d="m3.3 7 7.703 4.734a2 2 0 0 0 1.994 0L20.7 7"/><path d="m7.5 4.27 9 5.15"/>',
    key: '<path d="m21 2-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0 3 3L22 7l-3-3m-3.5 3.5L19 4"/>',
    play: '<polygon points="6 3 20 12 6 21 6 3"/>',
    check: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
    info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>',
    terminal: '<polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>',
    edit: '<path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/><path d="m15 5 4 4"/>',
    trash: '<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>',
    coffee: '<path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z"/><line x1="6" y1="2" x2="6" y2="4"/><line x1="10" y1="2" x2="10" y2="4"/><line x1="14" y1="2" x2="14" y2="4"/>',
    box: '<path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="m3.3 7 8.7 5 8.7-5"/><path d="M12 22V12"/>',
    hammer: '<path d="m15 12-8.373 8.373a1 1 0 1 1-3-3L12 9"/><path d="m18 15 4-4"/><path d="m21.5 11.5-1.914-1.914A2 2 0 0 1 19 8.172V7l-2.26-2.26a6 6 0 0 0-4.202-1.756L9 2.96l.92.82A6.18 6.18 0 0 1 12 8.4V10l2 2h1.172a2 2 0 0 1 1.414.586L18.5 14.5"/>',
    "alert-triangle": '<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
    database: '<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>',
    sparkles: '<path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/><path d="M20 3v4"/><path d="M22 5h-4"/>',
    sliders: '<line x1="21" x2="14" y1="4" y2="4"/><line x1="10" x2="3" y1="4" y2="4"/><line x1="21" x2="12" y1="12" y2="12"/><line x1="8" x2="3" y1="12" y2="12"/><line x1="21" x2="16" y1="20" y2="20"/><line x1="12" x2="3" y1="20" y2="20"/><line x1="14" x2="14" y1="2" y2="6"/><line x1="8" x2="8" y1="10" y2="14"/><line x1="16" x2="16" y1="18" y2="22"/>',
    settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
    wrench: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>',
    archive: '<rect width="20" height="5" x="2" y="3" rx="1"/><path d="M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8"/><path d="M10 12h4"/>',
    power: '<path d="M12 2v10"/><path d="M18.4 6.6a9 9 0 1 1-12.77.04"/>',
    refresh: '<path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>',
    moon: '<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>',
    monitor: '<rect width="20" height="14" x="2" y="3" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>',
    "file-archive": '<path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M16 22h4a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v18a2 2 0 0 0 2 2h2"/><path d="M10 12h.01"/><path d="M10 16h.01"/><path d="M10 20h.01"/>',
    download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
    "chevron-down": '<path d="m6 9 6 6 6-6"/>',
    "chevron-right": '<path d="m9 18 6-6-6-6"/>',
    paperclip: '<path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48"/>',
    "file-text": '<path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M16 22h4a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v18a2 2 0 0 0 2 2h2"/><path d="M14 18H9"/><path d="M18 14H9"/><path d="M14 10H9"/>'
  };

  function injectIcons(root) {
    root.querySelectorAll("[data-icon]").forEach(function (el) {
      var name = el.getAttribute("data-icon");
      if (ICONS[name]) el.innerHTML = ICONS[name];
    });
  }

  /* ---------- 工具函数 ---------- */
  var phone = document.getElementById("phone");
  var screens = document.querySelectorAll(".screen");
  var reviewChips = document.querySelectorAll("#reviewNav .chip");
  var snackbar = document.getElementById("snackbar");
  var snackTimer = null;

  function showSnack(msg) {
    snackbar.textContent = msg;
    snackbar.classList.add("show");
    clearTimeout(snackTimer);
    snackTimer = setTimeout(function () { snackbar.classList.remove("show"); }, 2600);
  }

  /* ---------- 屏幕导航 ---------- */
  function navigate(name) {
    screens.forEach(function (s) { s.classList.toggle("active", s.dataset.screen === name); });
    reviewChips.forEach(function (c) { c.classList.toggle("active", c.dataset.goto === name); });
    closeDrawer();
  }
  document.querySelectorAll("[data-goto]").forEach(function (el) {
    el.addEventListener("click", function () { navigate(el.dataset.goto); });
  });

  /* ---------- 抽屉 ---------- */
  var drawer = document.getElementById("drawer");
  var scrim = document.getElementById("scrim");
  function openDrawer() { drawer.classList.add("open"); scrim.classList.add("show"); }
  function closeDrawer() { drawer.classList.remove("open"); scrim.classList.remove("show"); }
  document.querySelector("[data-open-drawer]").addEventListener("click", openDrawer);
  scrim.addEventListener("click", closeDrawer);
  document.getElementById("drawerRestart").addEventListener("click", function () { showSnack("正在重启容器…"); });
  document.getElementById("drawerUpdate").addEventListener("click", function () { showSnack("已开始检查工具更新"); });

  /* ---------- 深色模式（跟随系统 + 手动三态） ---------- */
  var themeSeg = document.getElementById("themeSeg");
  var quickTheme = document.getElementById("quickTheme");
  var mode = "system"; // system | light | dark

  function resolvedDark() {
    if (mode === "dark") return true;
    if (mode === "light") return false;
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  }
  function applyTheme() {
    var dark = resolvedDark();
    phone.classList.toggle("dark", dark);
    document.body.classList.toggle("dark", dark);
    themeSeg.querySelectorAll(".seg-btn").forEach(function (b) {
      b.classList.toggle("active", b.dataset.themeChoice === mode);
    });
    var ic = quickTheme.querySelector(".ic");
    quickTheme.innerHTML = "<svg class=\"ic\" data-icon=\"" + (dark ? "sun" : "moon") + "\"></svg>";
    quickTheme.appendChild(document.createTextNode(" " + (dark ? "深色" : "浅色")));
    injectIcons(quickTheme);
  }
  themeSeg.querySelectorAll(".seg-btn").forEach(function (b) {
    b.addEventListener("click", function () { mode = b.dataset.themeChoice; applyTheme(); });
  });
  quickTheme.addEventListener("click", function () {
    mode = resolvedDark() ? "light" : "dark";
    applyTheme();
    showSnack("已切换为" + (mode === "dark" ? "深色" : "浅色") + "模式");
  });
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () {
    if (mode === "system") applyTheme();
  });

  /* ---------- 设置页：模型分段 ---------- */
  document.querySelectorAll(".seg").forEach(function (seg) {
    if (seg.id === "themeSeg") return;
    seg.querySelectorAll(".seg-btn").forEach(function (b) {
      b.addEventListener("click", function () {
        seg.querySelectorAll(".seg-btn").forEach(function (x) { x.classList.remove("active"); });
        b.classList.add("active");
      });
    });
  });

  /* ---------- 设置页：维护按钮 ---------- */
  document.getElementById("updateTools").addEventListener("click", function () { showSnack("正在更新工具（后台进行中）"); });
  document.getElementById("restartBtn").addEventListener("click", function () { showSnack("正在重启容器…"); });

  /* ---------- 网页主体：模型 / 思考强度（dsh ModelSelect 示意） ---------- */
  var modelMenu = document.getElementById("modelMenu");
  var wfModel = document.querySelector("[data-model-select]");
  var wfModelName = wfModel.querySelector(".wf-model-name");
  var wfEffort = wfModel.querySelector(".wf-effort");

  wfModel.addEventListener("click", function (e) {
    e.stopPropagation();
    modelMenu.hidden = !modelMenu.hidden;
  });
  document.addEventListener("click", function () { modelMenu.hidden = true; });
  modelMenu.addEventListener("click", function (e) { e.stopPropagation(); });
  document.querySelectorAll("[data-model-choice]").forEach(function (b) {
    b.addEventListener("click", function () {
      wfModelName.textContent = b.dataset.modelChoice;
      modelMenu.querySelectorAll("[data-model-choice]").forEach(function (x) { x.classList.toggle("active", x === b); });
      modelMenu.hidden = true;
      showSnack("已切换到模型 " + b.dataset.modelChoice);
    });
  });
  document.querySelectorAll("[data-effort-choice]").forEach(function (b) {
    b.addEventListener("click", function () {
      wfEffort.textContent = b.dataset.effortChoice;
      modelMenu.querySelectorAll("[data-effort-choice]").forEach(function (x) { x.classList.toggle("active", x === b); });
      modelMenu.hidden = true;
      showSnack("思考强度：已设为「" + b.dataset.effortChoice + "」");
    });
  });

  /* ---------- 文件发送（系统选择器，原型模拟） ---------- */
  document.querySelector("[data-file-send]").addEventListener("click", function () {
    showSnack("打开系统文件选择器…（真机走 SAF，选中后复制进容器）");
  });

  /* ---------- 设置页：模型与密钥入口（跳 dsh 网页主体） ---------- */
  document.querySelector("[data-goto-model-settings]").addEventListener("click", function () {
    navigate("chat");
    showSnack("模型 / 密钥 / 接口地址在 dsh 网页界面：设置 → 模型 中管理");
  });

  /* ---------- 工具下载源（镜像站） ---------- */
  var MIRROR_NAMES = { tsinghua: "清华 TUNA 镜像", aliyun: "阿里云镜像", ustc: "中科大镜像", official: "官方源" };
  document.querySelectorAll("#mirrorList input[name='mirror']").forEach(function (r) {
    r.addEventListener("change", function () {
      showSnack("下载源已切换为：" + MIRROR_NAMES[r.value]);
    });
  });

  /* ---------- 下载源：测速 ---------- */
  var SPEED_RESULTS = {
    tsinghua: { ms: 25,  status: "正常",     cls: "ok" },
    aliyun:   { ms: 48,  status: "正常",     cls: "ok" },
    ustc:     { ms: 128, status: "较慢",     cls: "" },
    official: { ms: 420, status: "可能很慢", cls: "warn" },
  };
  document.querySelectorAll("[data-speed]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var key = btn.dataset.speed;
      var row = btn.closest(".radio-row");
      var status = row.querySelector(".rr-status");
      var result = row.querySelector(".speed-result");
      btn.disabled = true;
      btn.textContent = "测速中…";
      result.textContent = "";
      setTimeout(function () {
        var r = SPEED_RESULTS[key];
        btn.disabled = false;
        btn.textContent = "测速";
        result.textContent = r.ms + " ms";
        status.textContent = r.status;
        status.className = "rr-status" + (r.cls ? " " + r.cls : "");
        showSnack(MIRROR_NAMES[key] + " 延迟约 " + r.ms + " ms");
      }, 900);
    });
  });

  /* ---------- 备份：打包内容勾选 ---------- */
  var backupSummary = document.getElementById("backupSummary");
  var ITEM_META = {
    config:  { name: "配置", mb: 4 },
    sessions: { name: "会话", mb: 12 },
    container: { name: "容器", mb: 2100 },
    tools:  { name: "工具", mb: 1900 },
  };
  function renderBackupSummary() {
    var picked = [];
    var mb = 0;
    document.querySelectorAll("#backupItems input[type='checkbox']").forEach(function (c) {
      if (c.checked) { picked.push(ITEM_META[c.dataset.item].name); mb += ITEM_META[c.dataset.item].mb; }
    });
    var size = mb >= 1024 ? (mb / 1024).toFixed(1) + " GB" : mb + " MB";
    backupSummary.innerHTML = '<svg class="ic sm" data-icon="info"></svg>当前将打包：<b>' +
      (picked.length > 0 ? picked.join(" + ") : "（未勾选任何内容）") +
      '</b>' + (picked.length > 0 ? "（约 " + size + "）" : "");
    injectIcons(backupSummary);
  }
  document.querySelectorAll("#backupItems input[type='checkbox']").forEach(function (c) {
    c.addEventListener("change", renderBackupSummary);
  });
  renderBackupSummary();

  /* ---------- 首次安装向导 ---------- */
  var wizStep = 1;
  var wizTimer = null;
  var wizProgress = document.getElementById("wizProgress");
  var wizText = document.getElementById("wizText");
  var wizPct = document.getElementById("wizPct");

  function clearWizTimer() { clearInterval(wizTimer); wizTimer = null; }

  function renderWiz() {
    document.querySelectorAll("#stepper .step").forEach(function (st) {
      var n = Number(st.dataset.step);
      st.classList.toggle("done", n < wizStep);
      st.classList.toggle("active", n === wizStep);
    });
    document.querySelectorAll(".wiz-pane").forEach(function (p) {
      p.classList.toggle("active", p.dataset.pane === String(wizStep));
    });
    document.getElementById("wizPrev").disabled = wizStep === 1;
    document.getElementById("wizNext").textContent = wizStep === 4 ? "开始使用" : "下一步";
  }

  function startStep1Progress() {
    clearWizTimer();
    var pct = parseInt(wizProgress.style.width, 10) || 64;
    wizTimer = setInterval(function () {
      pct = Math.min(pct + 2, 100);
      wizProgress.style.width = pct + "%";
      wizPct.textContent = pct + "%";
      wizText.textContent = "已解包 " + Math.round(520 * pct / 100) + " MB / 520 MB";
      if (pct >= 100) {
        clearWizTimer();
        setTimeout(function () { if (wizStep === 1) setWizStep(2); }, 600);
      }
    }, 90);
  }

  function setWizStep(n) {
    clearWizTimer();
    wizStep = n;
    renderWiz();
    if (n === 1) startStep1Progress();
    if (n === 3) {
      wizTimer = setTimeout(function () { if (wizStep === 3) setWizStep(4); }, 2500);
    }
  }

  document.getElementById("wizNext").addEventListener("click", function () {
    if (wizStep === 4) { setWizStep(1); navigate("chat"); return; }
    setWizStep(wizStep + 1);
  });
  document.getElementById("wizPrev").addEventListener("click", function () {
    if (wizStep > 1) setWizStep(wizStep - 1);
  });
  setWizStep(1);

  /* ---------- 工具管理 ---------- */
  var toolStates = { jdk: true, gradle: true, buildtools: true, ndk: false };
  var TOOL_NAMES = { jdk: "JDK", gradle: "Gradle", buildtools: "Build-Tools", ndk: "Android NDK" };

  function renderTools() {
    document.querySelectorAll(".tool-row").forEach(function (row) {
      var t = row.dataset.tool;
      var installed = toolStates[t];
      var tag = row.querySelector(".tag");
      tag.textContent = installed ? "已安装" : "未安装";
      tag.className = "tag " + (installed ? "ok" : "gray");
      var action = row.querySelector(".tool-action");
      if (action) action.remove();
      var btn = document.createElement("button");
      btn.className = installed ? "icon-btn sm tool-action" : "btn tonal sm tool-action";
      if (installed) {
        btn.setAttribute("aria-label", "卸载");
        btn.innerHTML = '<svg class="ic sm" data-icon="trash"></svg>';
        btn.addEventListener("click", function () {
          toolStates[t] = false; renderTools();
          showSnack("已卸载 " + TOOL_NAMES[t] + "，释放空间");
        });
      } else {
        btn.textContent = "安装";
        btn.addEventListener("click", function () {
          toolStates[t] = true; renderTools();
          showSnack("已开始安装 " + TOOL_NAMES[t] + "…");
        });
      }
      row.appendChild(btn);
      injectIcons(btn);
    });
  }
  renderTools();

  document.getElementById("installRecommended").addEventListener("click", function () {
    showSnack("正在安装推荐组合（AGP 8.7.2 + Gradle 8.9 + JDK 17）…");
  });

  /* ---------- 环境变量 ---------- */
  var envList = document.getElementById("envList");
  var ENV_NAMES = ["JAVA_HOME", "ANDROID_HOME", "ANDROID_SDK_ROOT", "ANDROID_NDK_HOME", "GRADLE_USER_HOME", "PATH"];

  function addEnvRow(key, value) {
    var row = document.createElement("div");
    row.className = "env-row";
    row.dataset.key = key;
    row.innerHTML =
      '<div class="env-k"><span class="env-key mono">' + key + '</span><span class="env-val mono">' + value + '</span></div>' +
      '<button class="icon-btn sm" data-edit-env aria-label="编辑"><svg class="ic sm" data-icon="edit"></svg></button>' +
      '<button class="icon-btn sm" data-del-env aria-label="删除"><svg class="ic sm" data-icon="trash"></svg></button>';
    bindEnvRow(row);
    envList.appendChild(row);
    injectIcons(row);
  }

  function bindEnvRow(row) {
    row.querySelector("[data-del-env]").addEventListener("click", function () {
      row.remove();
      showSnack("已删除 " + row.dataset.key + "（重启容器后生效）");
    });
    row.querySelector("[data-edit-env]").addEventListener("click", function () { openEnvModal(row.dataset.key, row); });
  }
  document.querySelectorAll(".env-row").forEach(bindEnvRow);

  function openEnvModal(key, row) {
    var modal = document.createElement("div");
    modal.className = "modal-scrim";
    var curVal = row ? row.querySelector(".env-val").textContent : "";
    modal.innerHTML =
      '<div class="modal">' +
      '<div class="modal-title">' + (row ? "编辑环境变量" : "新增环境变量") + '</div>' +
      '<div class="field"><label>变量名</label><input id="envKey" type="text" value="' + (key || "") + '" /></div>' +
      '<div class="field"><label>变量值</label><input id="envVal" type="text" value="' + curVal + '" /></div>' +
      '<div class="modal-actions">' +
      '<button class="btn tonal" id="envCancel">取消</button>' +
      '<button class="btn filled" id="envSave">保存</button>' +
      '</div></div>';
    phone.appendChild(modal);
    injectIcons(modal);
    document.getElementById("envCancel").addEventListener("click", function () { modal.remove(); });
    document.getElementById("envSave").addEventListener("click", function () {
      var k = document.getElementById("envKey").value.trim();
      var v = document.getElementById("envVal").value.trim();
      if (!k) { showSnack("变量名不能为空"); return; }
      if (row) {
        row.dataset.key = k;
        row.querySelector(".env-key").textContent = k;
        row.querySelector(".env-val").textContent = v;
      } else {
        addEnvRow(k, v);
      }
      modal.remove();
      showSnack("已保存（重启容器后生效）");
    });
  }
  document.getElementById("addEnv").addEventListener("click", function () { openEnvModal(null, null); });

  /* ---------- 备份 / 恢复 ---------- */
  var backupProgress = document.getElementById("backupProgress");
  var backupFill = document.getElementById("backupFill");
  var backupTimer = null;
  document.getElementById("doBackup").addEventListener("click", function () {
    clearInterval(backupTimer);
    backupProgress.classList.remove("hidden");
    var pct = 0;
    backupFill.style.width = "0%";
    backupTimer = setInterval(function () {
      pct = Math.min(pct + 3, 100);
      backupFill.style.width = pct + "%";
      if (pct >= 100) {
        clearInterval(backupTimer);
        setTimeout(function () { backupProgress.classList.add("hidden"); }, 600);
        showSnack("备份完成：已保存到 Download");
      }
    }, 70);
  });
  document.querySelectorAll("[data-restore]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      showSnack("恢复会覆盖当前环境，请先确认（原型演示）");
    });
  });

  /* ---------- 备份：删除（带确认弹窗） ---------- */
  function showConfirm(title, desc, onOk) {
    var modal = document.createElement("div");
    modal.className = "modal-scrim";
    modal.innerHTML =
      '<div class="modal">' +
      '<div class="modal-title">' + title + '</div>' +
      '<div class="modal-desc">' + desc + '</div>' +
      '<div class="modal-actions">' +
      '<button class="btn tonal" data-confirm-cancel>取消</button>' +
      '<button class="btn filled" style="background:var(--md-error);color:#fff;" data-confirm-ok>删除</button>' +
      '</div></div>';
    phone.appendChild(modal);
    injectIcons(modal);
    modal.querySelector("[data-confirm-cancel]").addEventListener("click", function () { modal.remove(); });
    modal.querySelector("[data-confirm-ok]").addEventListener("click", function () { modal.remove(); onOk(); });
  }
  document.querySelectorAll("[data-del-backup]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var row = btn.closest(".row");
      var name = row.querySelector(".row-label").textContent;
      var size = row.querySelector(".row-sub").textContent.split("·")[0].trim();
      showConfirm("删除此备份？", "将移除 " + name + "（" + size + "），不可恢复。", function () {
        row.remove();
        showSnack("已删除备份：" + name);
      });
    });
  });

  /* ---------- 初始化 ---------- */
  injectIcons(document);
  applyTheme();
})();
