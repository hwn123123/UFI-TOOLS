(() => {
    const icons = {
        overview: '<svg viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v10h13V10"/><path d="M9.5 20v-6h5v6"/></svg>',
        cellular: '<svg viewBox="0 0 24 24"><path d="M12 20V9"/><path d="m8.5 20 3.5-6 3.5 6"/><path d="M7.8 10.5a6 6 0 0 1 0-5"/><path d="M16.2 5.5a6 6 0 0 1 0 5"/><path d="M4.8 13.5a10 10 0 0 1 0-11"/><path d="M19.2 2.5a10 10 0 0 1 0 11"/></svg>',
        wireless: '<svg viewBox="0 0 24 24"><path d="M4.9 9.2a11 11 0 0 1 14.2 0"/><path d="M7.8 12.4a7 7 0 0 1 8.4 0"/><path d="M10.5 15.6a3 3 0 0 1 3 0"/><circle cx="12" cy="19" r="1"/></svg>',
        messages: '<svg viewBox="0 0 24 24"><path d="M5 5h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9l-5 3v-4a2 2 0 0 1-1-1V7a2 2 0 0 1 2-2Z"/><path d="M7 9h10M7 13h7"/></svg>',
        system: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 0 0 9 4.6 1.7 1.7 0 0 0 10 3v-.2h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z"/></svg>'
    }

    const pages = {
        overview: {
            label: '概览',
            title: '设备仪表盘',
            description: '设备状态、资源占用与网络信息',
            actions: [
                { label: '仪表盘', panel: 'overview', note: '网络、信号、资源与设备状态' },
                { label: '流量历史', target: 'DataUsageHistory', note: '日/月使用记录' }
            ]
        },
        cellular: {
            label: '蜂窝网络',
            title: '蜂窝网络',
            description: '信号、拨号、APN、频段与基站控制',
            actions: [
                { label: '网络概览', panel: 'status' },
                { label: '流量管理', target: 'DataManagement', note: '套餐与阈值' },
                { label: 'APN 设置', target: 'APNManagement', note: '拨号配置' },
                { label: '锁定频段', panel: 'band', note: '4G / 5G 频段' },
                { label: '锁定基站', panel: 'cell', note: 'PCI 与频点' },
                { label: '蜂窝测速', target: 'CellularSpeedTestBtn', note: '移动网络测速' },
                { label: '网络漫游', target: 'ROAM', note: '漫游开关' },
                { label: '数据连接', target: 'CECULLAR', note: '拨号开关' }
            ]
        },
        wireless: {
            label: 'Wi-Fi',
            title: 'Wi-Fi 与局域网',
            description: '无线网络、接入设备、局域网与 USB',
            actions: [
                { label: 'Wi-Fi 设置', target: 'WIFIManagement', note: '名称、密码与二维码' },
                { label: '接入设备', panel: 'clients', note: '真实客户端、流量、信号与限速' },
                { label: '局域网设置', target: 'LANManagement', note: 'DHCP 与网关' },
                { label: 'USB 模式', panel: 'usb', note: '原厂模式与 TetherKit 网卡模式' },
                { label: '内网测速', target: 'SpeedTestBtn', note: '局域网吞吐' },
                { label: '文件共享', target: 'SMB', note: 'SMB 服务' }
            ]
        },
        messages: {
            label: '短信',
            title: '短信中心',
            description: '收发短信、查看记录与配置转发',
            actions: [
                { label: '短信中心', panel: 'sms', note: 'SIM 短信、发送、存储与短信中心设置' },
                { label: '短信转发', target: 'smsForward', note: 'Webhook、邮件与规则' }
            ]
        },
        system: {
            label: '系统工具',
            title: '系统与高级工具',
            description: 'ADB、AT、插件、任务与设备维护',
            actions: [
                { label: '工具总览', panel: 'tools', note: '常用系统功能' },
                { label: 'ADB 调试', target: 'ADB', note: '调试接口状态' },
                { label: '高级功能', target: 'ADVANCE', note: 'Root 系统工具' },
                { label: 'AT 指令', target: 'AT', note: '模组命令终端' },
                { label: '插件功能', target: 'PLUGIN_SETTING', note: '插件与扩展' },
                { label: '定时任务', target: 'ScheduledTaskManagement', note: '自动化任务' },
                { label: '定时重启', target: 'SCHEDULE_REBOOT', note: '重启计划' },
                { label: '重启设备', target: 'REBOOT', note: '立即重新启动' },
                { label: '软件更新', target: 'OTA', note: '版本与更新' },
                { label: '主题设置', target: 'BG_SETTING', note: '颜色、背景与显示效果' },
                { label: '更改密码', target: 'CHANGEPWD', note: '厂家后台密码' },
                { label: '退出登录', target: 'CLEAR', note: '结束当前浏览器会话' },
                { label: '支持开发', panel: 'donate', note: '微信赞赏码' }
            ]
        }
    }

    const actionGlyphs = ['◇', '⌁', '◎', '⌗', '◫', '↗', '◉', '⌘', '△']
    const modalTargets = {
        DataUsageHistory: '#DataUsageHistoryModal',
        DataManagement: '#DataManagementModal',
        APNManagement: '#APNManagementModal',
        CellularSpeedTestBtn: '#CellularTestModal',
        WIFIManagement: '#WIFIManagementModal',
        ClientManagement: '#ClientManagementModal',
        LANManagement: '#LANManagementModal',
        USBStatusManagement: '#USBStatusModal',
        SpeedTestBtn: '#speedTestModal',
        SMS: '#smsList',
        smsForward: '#smsForwardModal',
        ADVANCE: '#advanceModal',
        AT: '#ATModal',
        PLUGIN_SETTING: '#PluginModal',
        ScheduledTaskManagement: '#ScheduledTasksModal',
        SCHEDULE_REBOOT: '#scheduleRebootModal',
        OTA: '#updateSoftwareModal',
        BG_SETTING: '#bgSettingModal',
        CHANGEPWD: '#changePassModal',
        CHANGETOKEN: '#changeTokenModal'
    }
    const dockableModalIds = new Set([
        ...Object.values(modalTargets).map(selector => selector.slice(1)),
        'AddTaskModal', 'bgSettingModal', 'plugin_store', 'editSinglePluginModal',
        'APNViewModal', 'APNEditModal', 'changePassModal', 'changeTokenModal', 'tokenModal',
        'TTYDModal', 'kano_net_info_modal', 'cpuTempInfoModal', 'memInfoModal'
    ])
    const state = {
        page: 'overview',
        panel: 'overview',
        activeAction: 'overview',
        dockMode: false,
        authenticated: Boolean(window.UFI_AUTHENTICATED)
    }

    function actionKey(action) {
        return action.panel || action.target || action.label
    }

        function safeValue(value, fallback = '--') {
            return value !== undefined && value !== null && String(value).trim() !== ''
                ? String(value)
                : fallback
        }

        function escapeHtml(value) {
            const span = document.createElement('span')
            span.textContent = value == null ? '' : String(value)
            return span.innerHTML
        }

    function networkLabel(data) {
        const raw = safeValue(data.network_type || data.network_information, '')
        if (raw === '20' || /5g|nr/i.test(raw)) return '5G'
        if (raw === '13' || /4g|lte/i.test(raw)) return '4G'
        return raw || '未注册'
    }

    function signalValue(data) {
        return safeValue(data.Z5g_rsrp || data.lte_rsrp || data.network_rssi || data.rssi)
    }

    function activeBand(data) {
        if (safeValue(data.Nr_bands, '') !== '') return `N${data.Nr_bands}`
        if (safeValue(data.Lte_bands, '') !== '') return `B${data.Lte_bands}`
        return '--'
    }

    function numberValue(value) {
        const parsed = Number.parseFloat(value)
        return Number.isFinite(parsed) ? parsed : null
    }

    function percentage(value) {
        const parsed = numberValue(value)
        return parsed === null ? 0 : Math.max(0, Math.min(100, parsed))
    }

    function metricQuality(value, worst, best) {
        const parsed = numberValue(value)
        if (parsed === null) return null
        return Math.max(0, Math.min(100, ((parsed - worst) / (best - worst)) * 100))
    }

    function signalQuality(data) {
        const metrics = [
            { score: metricQuality(signalValue(data), -120, -80), weight: .6 },
            { score: metricQuality(data.nr_rsrq || data.lte_rsrq, -20, -8), weight: .2 },
            { score: metricQuality(data.Nr_snr || data.Lte_snr, -10, 20), weight: .2 }
        ].filter(metric => metric.score !== null)
        if (!metrics.length) return 0
        const weight = metrics.reduce((total, metric) => total + metric.weight, 0)
        return Math.round(metrics.reduce((total, metric) => total + metric.score * metric.weight, 0) / weight)
    }

    function formatRate(value) {
        const parsed = numberValue(value)
        if (parsed === null || parsed <= 0) return '0 B/s'
        const units = ['B/s', 'KB/s', 'MB/s', 'GB/s']
        let amount = parsed
        let unit = 0
        while (amount >= 1024 && unit < units.length - 1) {
            amount /= 1024
            unit += 1
        }
        return `${amount >= 10 || unit === 0 ? amount.toFixed(0) : amount.toFixed(1)} ${units[unit]}`
    }

    function formatBytes(value) {
        const parsed = numberValue(value)
        if (parsed === null || parsed <= 0) return '0 B'
        const units = ['B', 'KB', 'MB', 'GB', 'TB']
        let amount = parsed
        let unit = 0
        while (amount >= 1024 && unit < units.length - 1) {
            amount /= 1024
            unit += 1
        }
        return `${amount >= 10 || unit === 0 ? amount.toFixed(0) : amount.toFixed(1)} ${units[unit]}`
    }

    function formatDuration(value) {
        let seconds = Math.max(0, Number.parseInt(value, 10) || 0)
        const days = Math.floor(seconds / 86400)
        seconds %= 86400
        const hours = Math.floor(seconds / 3600)
        seconds %= 3600
        const minutes = Math.floor(seconds / 60)
        if (days) return `${days} 天 ${hours} 小时`
        if (hours) return `${hours} 小时 ${minutes} 分`
        return `${minutes} 分 ${seconds % 60} 秒`
    }

    function simStatusLabel(value) {
        const raw = safeValue(value, '').toLowerCase()
        if (!raw || raw === 'unknown') return 'SIM 状态未知'
        if (raw === 'absent' || raw === 'nosim' || raw === 'no_sim') return '未插卡'
        if (raw === 'ready' || raw === 'available') return 'SIM 已就绪'
        if (raw.includes('pin')) return '需要 SIM PIN'
        return value
    }

    function activateOriginal(id) {
        const original = document.getElementById(id)
        if (!original) {
            window.createToast && createToast(`功能暂不可用：${id}`, 'pink')
            return
        }
        original.click()
    }

    function ensureOpen(panel) {
        const collapse = panel && panel.querySelector('.collapse')
        if (!collapse || collapse.dataset.name !== 'close') return
        const button = panel.querySelector('[id$="_btn"], #collapseBtn_menu')
        button && button.click()
    }

    function initialize() {
        const container = document.querySelector('.container')
        const originalTitle = container && container.querySelector(':scope > .main-title')
        if (!container || !originalTitle || document.querySelector('.u20-shell-header')) return

        document.body.classList.add('u20-dashboard')
        document.body.classList.toggle('u20-auth-locked', !state.authenticated)

        function resetDashboardPosition(resetTop = false) {
            const reset = () => {
                container.scrollLeft = 0
                document.documentElement.scrollLeft = 0
                document.body.scrollLeft = 0
                if (resetTop) container.scrollTop = 0
            }
            reset()
            window.requestAnimationFrame(reset)
        }

        resetDashboardPosition(true)
        window.addEventListener('resize', () => resetDashboardPosition(false), { passive: true })
        window.addEventListener('orientationchange', () => resetDashboardPosition(false), { passive: true })

        const header = document.createElement('header')
        header.className = 'u20-shell-header'
        header.innerHTML = `
            <div class="u20-brand"><div class="u20-brand-mark">U20</div><div class="u20-brand-slot"></div></div>
            <div class="u20-header-status">
                <span class="u20-status-chip" id="u20HeaderNetwork"><i class="u20-status-dot"></i>未注册</span>
                <span class="u20-status-chip" id="u20HeaderCarrier">运营商 --</span>
                <span class="u20-status-chip" id="u20HeaderClients">Wi-Fi 0</span>
                <div class="u20-header-actions">
                    <label class="u20-cloud-control u20-web-entry" title="开启后访问 192.168.88.1 会直接进入 UFI-TOOLS；关闭后恢复厂家首页">
                        <span><b>默认后台</b><small id="u20WebEntryState">默认开启</small></span>
                        <input type="checkbox" id="u20WebEntryToggle" checked>
                        <i aria-hidden="true"></i>
                    </label>
                    <label class="u20-cloud-control" title="开启后停止厂家 fly_p2xclient 云控服务；关闭可恢复官方服务">
                        <span><b>解除云控</b><small id="u20CloudState">默认开启</small></span>
                        <input type="checkbox" id="u20CloudToggle" checked>
                        <i aria-hidden="true"></i>
                    </label>
                    <div class="u20-action-menu">
                        <button type="button" class="u20-action-trigger" id="u20ActionTrigger" aria-label="设备与账户操作" aria-expanded="false">
                            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3"/><path d="M6.5 20a5.5 5.5 0 0 1 11 0"/><path d="M19 4v4m-2-2h4"/></svg>
                        </button>
                        <div class="u20-action-dropdown" id="u20ActionDropdown" hidden>
                            <button type="button" data-power-action="reboot"><span>↻</span><b>重启设备</b><small>正常重新启动系统</small></button>
                            <button type="button" data-power-action="shutdown" class="is-danger"><span>⏻</span><b>关闭设备</b><small>需要重新插电开机</small></button>
                            <button type="button" data-header-logout><span>⇥</span><b>退出登录</b><small>结束当前浏览器会话</small></button>
                        </div>
                    </div>
                </div>
            </div>`
        container.insertBefore(header, container.firstChild)
        header.querySelector('.u20-brand-slot').appendChild(originalTitle)

        const dialogLayer = document.createElement('div')
        dialogLayer.className = 'u20-dialog-layer'
        dialogLayer.hidden = true
        dialogLayer.innerHTML = `
            <section class="u20-dialog" role="dialog" aria-modal="true" aria-labelledby="u20DialogTitle">
                <div class="u20-dialog-icon" aria-hidden="true"></div>
                <h2 id="u20DialogTitle"></h2>
                <p id="u20DialogMessage"></p>
                <div class="u20-dialog-actions"></div>
            </section>`
        document.body.appendChild(dialogLayer)

        function closeCenteredDialog() {
            dialogLayer.hidden = true
            dialogLayer.className = 'u20-dialog-layer'
            dialogLayer.querySelector('.u20-dialog-actions').innerHTML = ''
        }

        function confirmCenteredDialog({ title, message, confirmText = '确认', danger = false }) {
            return new Promise(resolve => {
                dialogLayer.className = `u20-dialog-layer${danger ? ' is-danger' : ''}`
                dialogLayer.querySelector('#u20DialogTitle').textContent = title
                dialogLayer.querySelector('#u20DialogMessage').textContent = message
                const actions = dialogLayer.querySelector('.u20-dialog-actions')
                actions.innerHTML = '<button type="button" data-dialog-cancel>取消</button><button type="button" data-dialog-confirm></button>'
                actions.querySelector('[data-dialog-confirm]').textContent = confirmText
                const finish = result => {
                    closeCenteredDialog()
                    resolve(result)
                }
                actions.querySelector('[data-dialog-cancel]').onclick = () => finish(false)
                actions.querySelector('[data-dialog-confirm]').onclick = () => finish(true)
                dialogLayer.hidden = false
                actions.querySelector('[data-dialog-confirm]').focus()
            })
        }

        function showCenteredMessage({ title, message, danger = false, busy = false }) {
            dialogLayer.className = `u20-dialog-layer${danger ? ' is-danger' : ''}${busy ? ' is-busy' : ''}`
            dialogLayer.querySelector('#u20DialogTitle').textContent = title
            dialogLayer.querySelector('#u20DialogMessage').textContent = message
            const actions = dialogLayer.querySelector('.u20-dialog-actions')
            actions.innerHTML = busy ? '' : '<button type="button" data-dialog-close>知道了</button>'
            const closeButton = actions.querySelector('[data-dialog-close]')
            if (closeButton) closeButton.onclick = closeCenteredDialog
            dialogLayer.hidden = false
        }

        const primary = document.createElement('nav')
        primary.className = 'u20-primary-nav'
        primary.setAttribute('aria-label', '一级菜单')
        primary.innerHTML = Object.entries(pages).map(([key, page]) => `
            <button type="button" data-page="${key}">${icons[key]}<span>${page.label}</span></button>
        `).join('')
        header.insertAdjacentElement('afterend', primary)

        const subnavWrap = document.createElement('section')
        subnavWrap.className = 'u20-subnav-wrap'
        subnavWrap.innerHTML = `
            <div class="u20-section-label"><span>≡</span><strong id="u20SectionLabel">设备仪表盘</strong></div>
            <nav class="u20-subnav" aria-label="二级菜单"></nav>`
        primary.insertAdjacentElement('afterend', subnavWrap)

        const heading = document.createElement('section')
        heading.className = 'u20-page-heading'
        heading.innerHTML = `
            <div><h1 id="u20PageTitle"></h1><p id="u20PageDescription"></p></div>
            <span class="u20-live-label">设备实时数据</span>`
        subnavWrap.insertAdjacentElement('afterend', heading)

        const overviewPage = document.createElement('section')
        overviewPage.className = 'u20-overview-page'
        overviewPage.setAttribute('data-u20-overview', '')
        heading.insertAdjacentElement('afterend', overviewPage)

        const summary = document.createElement('section')
        summary.className = 'u20-overview-sections'
        summary.innerHTML = `
            <section class="u20-overview-group" data-overview-group="device">
                <div class="u20-overview-group-heading"><div><strong>设备监控</strong><small>CPU、内存、温度与实时流量</small></div><span>运行中</span></div>
                <div data-u20-device-monitor-slot></div>
                <div class="u20-summary-grid">
                    <article class="u20-summary-card u20-resource-card">
                        <div class="u20-summary-title"><span>设备资源</span><span id="u20ResourceTemp">--°C</span></div>
                        <div class="u20-resource-line"><span>CPU <b id="u20CpuValue">--%</b></span><i><em id="u20CpuBar"></em></i></div>
                        <div class="u20-resource-line"><span>内存 <b id="u20MemValue">--%</b></span><i><em id="u20MemBar"></em></i></div>
                        <div class="u20-summary-meta" id="u20MetricResourceMeta">等待设备数据</div>
                    </article>
                    <article class="u20-summary-card u20-traffic-card">
                        <div class="u20-summary-title"><span>实时网络吞吐</span><span id="u20MetricClients">0 台设备</span></div>
                        <div class="u20-traffic-rates"><strong>↓ <span id="u20DownSpeed">0 B/s</span></strong><strong>↑ <span id="u20UpSpeed">0 B/s</span></strong></div>
                        <div class="u20-wave" id="u20TrafficWave" aria-hidden="true">${Array.from({ length: 18 }, () => '<i></i>').join('')}</div>
                        <div class="u20-summary-meta" id="u20MetricClientsMeta">等待设备数据</div>
                    </article>
                </div>
            </section>
            <section class="u20-overview-group" data-overview-group="cellular">
                <div class="u20-overview-group-heading"><div><strong>蜂窝网络</strong><small>5G / 4G 注册、运营商与无线信号</small></div><span id="u20CellularState">等待网络</span></div>
                <div class="u20-summary-grid">
                    <article class="u20-summary-card u20-network-card">
                        <div class="u20-summary-title"><span>数据连接</span><span class="u20-pulse-dot" id="u20NetworkDot"></span></div>
                        <div class="u20-network-visual">
                            <div class="u20-radio-bars" id="u20RadioBars" aria-label="信号 0 格"><span></span><span></span><span></span><span></span><span></span></div>
                            <div><div class="u20-summary-value" id="u20MetricNetwork">--</div><div class="u20-summary-meta" id="u20MetricNetworkMeta">等待设备数据</div></div>
                        </div>
                    </article>
                    <article class="u20-summary-card u20-signal-card">
                        <div class="u20-summary-title"><span>信号质量</span><span id="u20SimBadge">SIM 状态未知</span></div>
                        <div class="u20-gauge-row">
                            <div class="u20-signal-gauge" id="u20SignalGauge" style="--gauge:0deg"><span id="u20SignalQuality">0</span><small>%</small></div>
                            <div><div class="u20-summary-value" id="u20MetricSignal">--</div><div class="u20-summary-meta" id="u20MetricSignalMeta">SINR -- · RSRQ --</div></div>
                        </div>
                    </article>
                </div>
                <div data-u20-signal-slot></div>
            </section>
            <section class="u20-overview-group" data-overview-group="wifi">
                <div class="u20-overview-group-heading"><div><strong>Wi-Fi 网络</strong><small>无线状态、已连设备与管理入口</small></div><span id="u20WifiState">Wi-Fi 已开启</span></div>
                <div class="u20-summary-grid u20-wifi-grid">
                    <article class="u20-summary-card u20-wifi-card">
                        <div class="u20-summary-title"><span>无线连接</span><span class="u20-wifi-pulse" id="u20WifiPulse"></span></div>
                        <div class="u20-wifi-visual"><div class="u20-wifi-radar" id="u20WifiRadar"><i></i><i></i><i></i><b></b></div><div><div class="u20-summary-value" id="u20WifiClients">0 台设备</div><div class="u20-summary-meta" id="u20WifiGateway">网关 192.168.88.1</div></div></div>
                    </article>
                    <article class="u20-summary-card u20-wifi-info-card">
                        <div class="u20-summary-title"><span>局域网</span><span>2.4G / 5G</span></div>
                        <div class="u20-wifi-info-lines"><span><small>管理地址</small><strong id="u20WifiAdminAddress">192.168.88.1</strong></span><span><small>默认后台</small><strong>:2333</strong></span></div>
                    </article>
                </div>
            </section>
            <section class="u20-overview-group" data-overview-group="other">
                <div class="u20-overview-group-heading"><div><strong>其他设备信息</strong><small>SIM、版本、地址与运行状态</small></div><span>详细信息</span></div>
                <div data-u20-device-status-slot></div>
            </section>`
        overviewPage.appendChild(summary)

        const deviceOverviewGroup = summary.querySelector('[data-overview-group="device"]')
        const cellularOverviewGroup = summary.querySelector('[data-overview-group="cellular"]')
        const wifiOverviewGroup = summary.querySelector('[data-overview-group="wifi"]')
        const otherOverviewGroup = summary.querySelector('[data-overview-group="other"]')
        const overviewGroups = [deviceOverviewGroup, cellularOverviewGroup, wifiOverviewGroup, otherOverviewGroup]

        const signalDetails = document.createElement('section')
        signalDetails.className = 'u20-signal-details'
        signalDetails.innerHTML = `
            <div class="u20-card-heading"><div><strong>蜂窝信号参数</strong><small>无卡或未注册时保留参数位置，数值显示为 --</small></div><span id="u20SignalState">等待设备</span></div>
            <div class="u20-signal-facts">
                <article id="u20FactRsrpCard"><span>RSRP</span><strong id="u20FactRsrp">--</strong><small>dBm</small></article>
                <article id="u20FactRsrqCard"><span>RSRQ</span><strong id="u20FactRsrq">--</strong><small>dB</small></article>
                <article id="u20FactSinrCard"><span>SINR</span><strong id="u20FactSinr">--</strong><small>dB</small></article>
                <article><span>频段</span><strong id="u20FactBand">--</strong><small>Band</small></article>
                <article><span>PCI</span><strong id="u20FactPci">--</strong><small>Physical Cell ID</small></article>
                <article><span>拨号</span><strong id="u20FactPpp">--</strong><small>数据会话</small></article>
            </div>`
        cellularOverviewGroup.querySelector('[data-u20-signal-slot]').appendChild(signalDetails)

        const actionPanel = document.createElement('section')
        actionPanel.className = 'u20-empty-panel'
        actionPanel.hidden = true
        overviewPage.insertAdjacentElement('afterend', actionPanel)

        const inlineHost = document.createElement('section')
        inlineHost.className = 'u20-inline-host'
        inlineHost.hidden = true
        inlineHost.innerHTML = '<div class="u20-inline-static"></div><div class="u20-inline-dock"></div>'
        actionPanel.insertAdjacentElement('afterend', inlineHost)
        const inlineStatic = inlineHost.querySelector('.u20-inline-static')
        const inlineDock = inlineHost.querySelector('.u20-inline-dock')
        let defaultActionTimer = null
        let wifiClientRefreshTimer = null
        let cloudRequestPending = false
        let webEntryRequestPending = false

        async function syncWebEntry(enabled = null) {
            if (!state.authenticated || webEntryRequestPending) return
            const toggle = document.getElementById('u20WebEntryToggle')
            const status = document.getElementById('u20WebEntryState')
            webEntryRequestPending = true
            toggle.disabled = true
            status.textContent = enabled === null ? '读取中' : '设置中'
            try {
                const options = enabled === null ? {
                    headers: { ...common_headers }
                } : {
                    method: 'POST',
                    headers: { ...common_headers, 'Content-Type': 'application/json' },
                    body: JSON.stringify({ enabled })
                }
                const response = await fetch(`${KANO_baseURL}/u20/web_entry`, options)
                const data = await response.json()
                if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                toggle.checked = Boolean(data.enabled)
                status.textContent = data.enabled ? (data.active ? '进入 2333' : '等待生效') : '厂家首页'
                toggle.closest('.u20-web-entry').classList.toggle('is-active', Boolean(data.enabled && data.active))
            } catch (error) {
                if (enabled !== null) toggle.checked = !enabled
                status.textContent = '操作失败'
                window.createToast && createToast(`默认后台设置失败：${error.message}`, 'red')
            } finally {
                webEntryRequestPending = false
                toggle.disabled = false
            }
        }

        document.getElementById('u20WebEntryToggle').addEventListener('change', event => {
            syncWebEntry(Boolean(event.target.checked))
        })

        async function syncCloudControl(released = null) {
            if (!state.authenticated || cloudRequestPending) return
            const toggle = document.getElementById('u20CloudToggle')
            const status = document.getElementById('u20CloudState')
            cloudRequestPending = true
            toggle.disabled = true
            status.textContent = released === null ? '读取中' : '设置中'
            try {
                const options = released === null ? {
                    headers: { ...common_headers }
                } : {
                    method: 'POST',
                    headers: { ...common_headers, 'Content-Type': 'application/json' },
                    body: JSON.stringify({ released })
                }
                const response = await fetch(`${KANO_baseURL}/u20/cloud_control`, options)
                const data = await response.json()
                if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                toggle.checked = Boolean(data.released)
                status.textContent = data.released ? '已解除' : '官方模式'
                toggle.closest('.u20-cloud-control').classList.toggle('is-released', Boolean(data.released))
            } catch (error) {
                if (released !== null) toggle.checked = !released
                status.textContent = '操作失败'
                window.createToast && createToast(`云控设置失败：${error.message}`, 'red')
            } finally {
                cloudRequestPending = false
                toggle.disabled = false
            }
        }

        document.getElementById('u20CloudToggle').addEventListener('change', event => {
            syncCloudControl(Boolean(event.target.checked))
        })

        const actionMenu = header.querySelector('.u20-action-menu')
        const actionTrigger = document.getElementById('u20ActionTrigger')
        const actionDropdown = document.getElementById('u20ActionDropdown')
        function closeActionMenu() {
            actionDropdown.hidden = true
            actionTrigger.setAttribute('aria-expanded', 'false')
        }
        actionTrigger.addEventListener('click', event => {
            event.stopPropagation()
            const willOpen = actionDropdown.hidden
            actionDropdown.hidden = !willOpen
            actionTrigger.setAttribute('aria-expanded', String(willOpen))
        })
        actionDropdown.addEventListener('click', event => event.stopPropagation())
        document.addEventListener('click', event => {
            if (!actionMenu.contains(event.target)) closeActionMenu()
        })
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') closeActionMenu()
        })
        actionDropdown.querySelector('[data-header-logout]').addEventListener('click', () => {
            closeActionMenu()
            activateOriginal('CLEAR')
        })

        async function requestPowerAction(action) {
            if (!state.authenticated) {
                showModal('#tokenModal')
                return
            }
            const isShutdown = action === 'shutdown'
            const actionName = isShutdown ? '关机' : '重启'
            const warning = isShutdown
                ? '确定要关闭设备电源吗？关机后需要重新插拔电源才能启动。'
                : '确定要立即重启设备吗？网络会暂时断开。'
            closeActionMenu()
            const confirmed = await confirmCenteredDialog({
                title: `${actionName}设备`,
                message: warning,
                confirmText: `确认${actionName}`,
                danger: isShutdown
            })
            if (!confirmed) return

            const buttons = [...header.querySelectorAll('[data-power-action]')]
            buttons.forEach(button => { button.disabled = true })
            showCenteredMessage({ title: `正在${actionName}`, message: '正在向设备发送电源命令…', busy: true })
            try {
                const response = await fetch(`${KANO_baseURL}/u20/power`, {
                    method: 'POST',
                    headers: { ...common_headers, 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action })
                })
                const result = await response.json()
                if (!response.ok || result.error) throw new Error(result.error || `HTTP ${response.status}`)
                showCenteredMessage({
                    title: `${actionName}命令已发送`,
                    message: isShutdown
                        ? '设备将在 2 秒后正常关机，之后需要重新插拔电源启动。'
                        : '设备将在 2 秒后重启，页面和网络会暂时断开。'
                })
            } catch (error) {
                buttons.forEach(button => { button.disabled = false })
                showCenteredMessage({ title: `${actionName}失败`, message: error.message, danger: true })
            }
        }

        header.querySelectorAll('[data-power-action]').forEach(button => {
            button.addEventListener('click', () => requestPowerAction(button.dataset.powerAction))
        })

        const footer = document.querySelector('.footer')
        if (footer) {
            footer.innerHTML = '<p>Made by <strong>ningge</strong> with love ❤️&nbsp;<button type="button" id="u20DonateLink">支持开发</button></p>'
        }
        const legacyPayModal = document.getElementById('payModal')
        legacyPayModal && (legacyPayModal.style.display = 'none')
        localStorage.setItem('hidePayAndGroupModal', 'true')

        const panelMap = {
            monitor: container.querySelector('.devices-mon'),
            status: container.querySelector('.status-container'),
            ttyd: container.querySelector('#TTYD'),
            band: container.querySelector('.band-lock-container'),
            cell: container.querySelector('.freq-lock-container')
        }
        Object.values(panelMap).forEach(panel => panel && panel.setAttribute('data-u20-panel', ''))
        panelMap.monitor && deviceOverviewGroup.querySelector('[data-u20-device-monitor-slot]').appendChild(panelMap.monitor)
        panelMap.status && otherOverviewGroup.querySelector('[data-u20-device-status-slot]').appendChild(panelMap.status)

        function hideDockedFeatures(except = null) {
            inlineDock.querySelectorAll('[data-u20-docked="true"]').forEach(element => {
                if (element !== except) element.style.display = 'none'
            })
        }

        function dockFeature(element) {
            if (!element || !state.dockMode || element.id === 'payModal') return
            const isNewDock = element.parentElement !== inlineDock
            hideDockedFeatures(element)
            element.dataset.u20Docked = 'true'
            if (isNewDock) inlineDock.appendChild(element)
            inlineStatic.hidden = true
            inlineHost.hidden = false
            if (element.style.display !== 'block') element.style.display = 'block'
            if (element.style.opacity !== '1') element.style.opacity = '1'
            if (element.style.transform !== 'none') element.style.transform = 'none'
        }

        function scanForDockable() {
            const known = [...dockableModalIds].map(id => document.getElementById(id))
            const generated = [...container.children].filter(element =>
                (element.classList.contains('mask') || element.classList.contains('modal')) &&
                element.id !== 'payModal'
            )
            const candidates = [...known, ...generated].filter(element =>
                state.authenticated || element?.id === 'tokenModal'
            )
            for (const element of candidates) {
                if (!element || element.style.display === 'none') continue
                if (!state.dockMode) {
                    state.dockMode = true
                    state.panel = ''
                    overviewPage.hidden = true
                    summary.hidden = true
                    Object.values(panelMap).forEach(panel => panel && (panel.hidden = true))
                }
                dockFeature(element)
                return
            }
        }

        const modalObserver = new MutationObserver(mutations => {
            const shouldScan = mutations.some(mutation =>
                mutation.type === 'childList' ||
                (mutation.type === 'attributes' && mutation.attributeName === 'style')
            )
            shouldScan && window.requestAnimationFrame(scanForDockable)
        })
        modalObserver.observe(document.body, {
            subtree: true,
            childList: true,
            attributes: true,
            attributeFilter: ['style']
        })

        function renderDonatePage() {
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            inlineStatic.innerHTML = `
                <article class="u20-inline-page u20-donate-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>支持开发</h2><p>感谢你对 U20 适配与后续功能开发的支持</p></div>
                    </div>
                    <a class="u20-donate-code" href="/img/ningge-wechat-pay.png" target="_blank" title="点击查看原图">
                        <img src="/img/ningge-wechat-pay.png" alt="ningge 微信赞赏码">
                    </a>
                    <p class="u20-donate-tip">微信扫码赞赏 · 点击二维码可查看原图</p>
                </article>`
        }

        function renderCommandPage(action) {
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            inlineStatic.innerHTML = `
                <article class="u20-inline-page u20-command-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>${action.label}</h2><p>${action.note || '设备功能操作'}</p></div>
                    </div>
                    <div class="u20-command-card">
                        <span class="u20-action-card-icon">${actionGlyphs[1]}</span>
                        <div><strong>${action.label}</strong><small>确认后由设备执行该操作</small></div>
                        <button type="button" id="u20RunCommand">执行操作</button>
                    </div>
                </article>`
            inlineStatic.querySelector('#u20RunCommand').addEventListener('click', () => activateOriginal(action.target))
        }

        function renderToolsPage() {
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            const actions = pages.system.actions.filter(action => action.panel !== 'tools')
            inlineStatic.innerHTML = `
                <article class="u20-inline-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>系统工具</h2><p>选择需要使用的维护、调试或扩展功能</p></div>
                    </div>
                    <div class="u20-tool-grid">
                        ${actions.map((action, index) => `
                            <button type="button" class="u20-action-card" data-system-action="${index}">
                                <span class="u20-action-card-icon">${actionGlyphs[index % actionGlyphs.length]}</span>
                                <span><strong>${action.label}</strong><small>${action.note || '系统功能'}</small></span>
                            </button>`).join('')}
                    </div>
                </article>`
            inlineStatic.querySelectorAll('[data-system-action]').forEach(button => {
                button.addEventListener('click', () => runAction(actions[Number(button.dataset.systemAction)]))
            })
        }

        async function renderSmsPage() {
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            inlineStatic.innerHTML = `
                <article class="u20-inline-page u20-sms-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>短信中心</h2><p>直接读取 SIM 卡与设备短信，发送结果以基带回执为准</p></div>
                        <button type="button" id="u20SmsRefresh">刷新全部</button>
                    </div>
                    <div class="u20-sms-status-grid">
                        <span><small>SIM 状态</small><strong id="u20SmsSimState">读取中…</strong></span>
                        <span><small>短信存储</small><strong id="u20SmsStorageState">-- / --</strong></span>
                        <span><small>短信中心</small><strong id="u20SmsCenterState">--</strong></span>
                        <span><small>语音注册</small><strong id="u20VoiceState">检测中…</strong></span>
                    </div>
                    <section class="u20-sms-compose">
                        <div class="u20-card-heading"><div><strong>发送短信</strong><small>支持中文和长短信；提交后等待基带返回真实结果</small></div><span id="u20SmsSendState">就绪</span></div>
                        <div class="u20-sms-compose-grid">
                            <label><span>收件号码</span><input id="u20SmsNumber" type="tel" inputmode="tel" autocomplete="tel" placeholder="输入手机号码"></label>
                            <label class="u20-sms-message-field"><span>短信内容</span><textarea id="u20SmsBody" rows="4" maxlength="1000" placeholder="输入短信内容"></textarea><small id="u20SmsCounter">0 字 · 0 条</small></label>
                            <button type="button" id="u20SmsSend">发送短信</button>
                        </div>
                    </section>
                    <section class="u20-sms-settings">
                        <div class="u20-card-heading"><div><strong>短信与 SIM 存储设置</strong><small>SM 为 SIM 卡，ME 为设备；当前固件支持两种存储</small></div><button type="button" id="u20SmsUseSim">全部使用 SIM</button></div>
                        <div class="u20-sms-settings-grid">
                            <label><span>短信中心号码</span><input id="u20SmsCenter" type="tel" inputmode="tel" placeholder="例如 +8613800100500"></label>
                            <label><span>读取/列表存储</span><select id="u20SmsReadStore"><option value="SM">SIM 卡（SM）</option><option value="ME">设备（ME）</option></select></label>
                            <label><span>发送/写入存储</span><select id="u20SmsWriteStore"><option value="SM">SIM 卡（SM）</option><option value="ME">设备（ME）</option></select></label>
                            <label><span>接收存储</span><select id="u20SmsReceiveStore"><option value="SM">SIM 卡（SM）</option><option value="ME">设备（ME）</option></select></label>
                            <button type="button" id="u20SmsSaveSettings">保存设置</button>
                        </div>
                    </section>
                    <section class="u20-sms-inbox">
                        <div class="u20-card-heading">
                            <div><strong>短信记录</strong><small id="u20SmsListSummary">正在读取 SIM 卡…</small></div>
                            <div class="u20-sms-list-actions"><input id="u20SmsSearch" type="search" placeholder="搜索号码或内容"><select id="u20SmsFilter"><option value="all">全部短信</option><option value="unread">未读</option><option value="received">已接收</option><option value="sent">已发送</option><option value="failed">发送失败</option></select><button type="button" id="u20SmsExport">导出备份</button></div>
                        </div>
                        <div class="u20-sms-list" id="u20SmsList"><p class="u20-sms-empty">正在读取短信…</p></div>
                    </section>
                    <section class="u20-voice-diagnostic">
                        <div class="u20-card-heading"><div><strong>电话与浏览器音频</strong><small>检测系统拨号、语音网络和音频通道</small></div><span id="u20VoiceBadge">检测中</span></div>
                        <p id="u20VoiceDescription">正在检测设备是否具备可用的蜂窝语音链路…</p>
                    </section>
                </article>`

            const root = inlineStatic.querySelector('.u20-sms-page')
            let messages = []
            let status = null
            const get = selector => root.querySelector(selector)
            const setText = (selector, value) => {
                const node = get(selector)
                if (node) node.textContent = value
            }
            const decodeBody = value => {
                try {
                    const bytes = Uint8Array.from(atob(value || ''), char => char.charCodeAt(0))
                    return new TextDecoder('utf-8').decode(bytes)
                } catch (_) {
                    return value || ''
                }
            }
            const formatDate = raw => {
                const parts = String(raw || '').split(',').map(Number)
                if (parts.length < 6 || parts.some((value, index) => index < 6 && !Number.isFinite(value))) return raw || '--'
                return `${parts[0]}-${String(parts[1]).padStart(2, '0')}-${String(parts[2]).padStart(2, '0')} ${String(parts[3]).padStart(2, '0')}:${String(parts[4]).padStart(2, '0')}`
            }
            const tagInfo = tag => ({
                '0': ['已读', 'received'],
                '1': ['未读', 'unread'],
                '2': ['已发送', 'sent'],
                '3': ['发送失败', 'failed']
            }[String(tag)] || ['短信', 'received'])
            const normalizeSmsNumber = number => {
                const digits = String(number || '').replace(/\D/g, '')
                return digits.startsWith('86') && digits.length === 13 ? digits.slice(2) : (digits || String(number || '未知号码'))
            }
            const showError = (title, error) => showCenteredMessage({
                title,
                message: error?.message || String(error),
                danger: true
            })

            function renderMessages() {
                if (!root.isConnected) return
                const query = get('#u20SmsSearch').value.trim().toLowerCase()
                const filter = get('#u20SmsFilter').value
                const filtered = messages.filter(message => {
                    const body = decodeBody(message.content)
                    const [, kind] = tagInfo(message.tag)
                    const matchesFilter = filter === 'all' || kind === filter || (filter === 'received' && (kind === 'received' || kind === 'unread'))
                    const matchesQuery = !query || `${message.number} ${body}`.toLowerCase().includes(query)
                    return matchesFilter && matchesQuery
                })
                const conversations = []
                const conversationMap = new Map()
                filtered.forEach(message => {
                    const key = normalizeSmsNumber(message.number)
                    let conversation = conversationMap.get(key)
                    if (!conversation) {
                        conversation = { key, number: message.number || '未知号码', messages: [] }
                        conversationMap.set(key, conversation)
                        conversations.push(conversation)
                    }
                    conversation.messages.push(message)
                })
                setText('#u20SmsListSummary', `${messages.length} 条短信 · ${conversations.length} 个会话`)
                const list = get('#u20SmsList')
                if (!conversations.length) {
                    list.innerHTML = '<p class="u20-sms-empty">当前条件下没有短信</p>'
                    return
                }
                list.innerHTML = conversations.map(conversation => {
                    const unreadIds = conversation.messages
                        .filter(message => tagInfo(message.tag)[1] === 'unread')
                        .map(message => message.id)
                        .join(',')
                    const latest = conversation.messages[0]
                    const bubbles = [...conversation.messages].reverse().map(message => {
                        const body = decodeBody(message.content)
                        const [label, kind] = tagInfo(message.tag)
                        const actualParts = Number(message.parts) || 1
                        const expectedParts = Number(message.expected_parts) || actualParts
                        const multipartLabel = expectedParts > 1 ? ` · 长短信 ${actualParts}/${expectedParts} 段` : ''
                        return `<article class="u20-sms-bubble is-${kind}" data-sms-id="${escapeHtml(message.id)}">
                            <div><span>${escapeHtml(formatDate(message.date))}</span><i>${escapeHtml(message.store || '设备')} · ${escapeHtml(label)}${multipartLabel}</i></div>
                            <p>${escapeHtml(body)}</p>
                            <button type="button" data-sms-delete="${escapeHtml(message.id)}" class="is-danger">删除</button>
                        </article>`
                    }).join('')
                    return `<section class="u20-sms-thread" data-sms-thread="${escapeHtml(conversation.key)}">
                        <header>
                            <div><strong>${escapeHtml(conversation.number)}</strong><span>${conversation.messages.length} 条 · 最近 ${escapeHtml(formatDate(latest.date))}</span></div>
                            <div class="u20-sms-item-actions">
                                <button type="button" data-thread-reply="${escapeHtml(conversation.number)}">回复</button>
                                ${unreadIds ? `<button type="button" data-thread-read="${escapeHtml(unreadIds)}">全部标为已读</button>` : ''}
                            </div>
                        </header>
                        <div class="u20-sms-thread-messages">${bubbles}</div>
                    </section>`
                }).join('')
                list.querySelectorAll('[data-thread-reply]').forEach(button => button.addEventListener('click', () => {
                    get('#u20SmsNumber').value = button.dataset.threadReply || ''
                    get('#u20SmsBody').focus()
                    root.scrollIntoView({ behavior: 'smooth', block: 'start' })
                }))
                list.querySelectorAll('[data-thread-read]').forEach(button => button.addEventListener('click', async () => {
                    await mutateMessage('SET_MSG_READ', button.dataset.threadRead)
                }))
                list.querySelectorAll('[data-sms-delete]').forEach(button => button.addEventListener('click', async () => {
                    const confirmed = await confirmCenteredDialog({ title: '删除短信', message: '确定删除这条短信吗？删除后无法恢复。', confirmText: '确认删除', danger: true })
                    if (confirmed) await mutateMessage('DELETE_SMS', button.dataset.smsDelete)
                }))
            }

            async function loadStatus() {
                const response = await fetch(`${KANO_baseURL}/u20/sms/status`, { headers: { ...common_headers } })
                const data = await response.json()
                if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                status = data
                setText('#u20SmsSimState', data.sim_ready ? `${data.carrier || 'SIM'} · 卡槽 ${Number(data.sim_slot) + 1}` : '未检测到 SIM')
                setText('#u20SmsStorageState', `${data.read_store} ${data.read_used}/${data.read_total}`)
                setText('#u20SmsCenterState', data.smsc || '未设置')
                setText('#u20VoiceState', data.voice_registered ? '已注册' : '未注册')
                get('#u20SmsCenter').value = data.smsc || ''
                get('#u20SmsReadStore').value = data.read_store === 'ME' ? 'ME' : 'SM'
                get('#u20SmsWriteStore').value = data.write_store === 'ME' ? 'ME' : 'SM'
                get('#u20SmsReceiveStore').value = data.receive_store === 'ME' ? 'ME' : 'SM'
                const voiceReady = Boolean(data.voice_registered)
                setText('#u20VoiceBadge', voiceReady ? '语音已注册' : '当前不可用')
                setText('#u20VoiceDescription', voiceReady
                    ? '基带已经注册语音网络，但浏览器通话仍需要额外的双向音频桥接服务，暂未开放拨号。'
                    : `Android 电话框架存在，但蜂窝语音未注册（CS=${data.cs_registration}）；厂家拨号器还缺少语音信箱组件且音频通道初始化失败，因此目前不能可靠拨号，也不能把浏览器话筒/听筒接入蜂窝通话。`)
            }

            async function loadMessages() {
                const response = await fetch(`${KANO_baseURL}/goform/goform_get_cmd_process?multi_data=1&cmd=sms_data_total&page=0&data_per_page=500&_=${Date.now()}`, { headers: { ...common_headers } })
                const data = await response.json()
                if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                messages = Array.isArray(data.messages) ? data.messages : []
                renderMessages()
            }

            async function mutateMessage(goformId, id) {
                try {
                    const body = new URLSearchParams({ goformId, msg_id: id, notCallback: 'true' })
                    const response = await fetch(`${KANO_baseURL}/goform/goform_set_cmd_process`, {
                        method: 'POST',
                        headers: { ...common_headers, 'Content-Type': 'application/x-www-form-urlencoded' },
                        body
                    })
                    const data = await response.json()
                    if (!response.ok || data.result !== 'success') throw new Error(data.message || `HTTP ${response.status}`)
                    await loadMessages()
                } catch (error) {
                    showError('短信操作失败', error)
                }
            }

            async function refreshAll() {
                const button = get('#u20SmsRefresh')
                button.disabled = true
                try {
                    await Promise.all([loadStatus(), loadMessages()])
                } catch (error) {
                    showError('读取短信失败', error)
                } finally {
                    button.disabled = false
                }
            }

            get('#u20SmsRefresh').addEventListener('click', refreshAll)
            get('#u20SmsSearch').addEventListener('input', renderMessages)
            get('#u20SmsFilter').addEventListener('change', renderMessages)
            get('#u20SmsBody').addEventListener('input', event => {
                const length = [...event.target.value].length
                const singleLimit = /[^\x00-\x7F]/.test(event.target.value) ? 70 : 160
                const multipartLimit = /[^\x00-\x7F]/.test(event.target.value) ? 67 : 153
                const parts = length === 0 ? 0 : (length <= singleLimit ? 1 : Math.ceil(length / multipartLimit))
                setText('#u20SmsCounter', `${length} 字 · ${parts} 条`)
            })
            get('#u20SmsSend').addEventListener('click', async () => {
                const number = get('#u20SmsNumber').value.trim()
                const content = get('#u20SmsBody').value.trim()
                if (!/^\+?[0-9]{3,32}$/.test(number) || !content) {
                    return showCenteredMessage({ title: '无法发送', message: '请填写正确的收件号码和短信内容。', danger: true })
                }
                const button = get('#u20SmsSend')
                button.disabled = true
                setText('#u20SmsSendState', '等待基带回执…')
                try {
                    const body = new URLSearchParams({ goformId: 'SEND_SMS', Number: number, MessageBody: gsmEncode(content) })
                    const response = await fetch(`${KANO_baseURL}/goform/goform_set_cmd_process`, {
                        method: 'POST',
                        headers: { ...common_headers, 'Content-Type': 'application/x-www-form-urlencoded' },
                        body
                    })
                    const data = await response.json()
                    if (!response.ok || data.result !== 'success') throw new Error(data.message || `HTTP ${response.status}`)
                    setText('#u20SmsSendState', data.message || '基带已接受')
                    get('#u20SmsBody').value = ''
                    get('#u20SmsBody').dispatchEvent(new Event('input'))
                    await loadMessages()
                    showCenteredMessage({ title: '短信已提交', message: data.message || '基带已接受短信。' })
                } catch (error) {
                    setText('#u20SmsSendState', '发送失败')
                    showError('短信发送失败', error)
                } finally {
                    button.disabled = false
                }
            })
            get('#u20SmsUseSim').addEventListener('click', () => {
                ;['#u20SmsReadStore', '#u20SmsWriteStore', '#u20SmsReceiveStore'].forEach(selector => { get(selector).value = 'SM' })
            })
            get('#u20SmsSaveSettings').addEventListener('click', async () => {
                const button = get('#u20SmsSaveSettings')
                button.disabled = true
                try {
                    const response = await fetch(`${KANO_baseURL}/u20/sms/settings`, {
                        method: 'POST',
                        headers: { ...common_headers, 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            smsc: get('#u20SmsCenter').value.trim(),
                            read_store: get('#u20SmsReadStore').value,
                            write_store: get('#u20SmsWriteStore').value,
                            receive_store: get('#u20SmsReceiveStore').value
                        })
                    })
                    const data = await response.json()
                    if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                    await refreshAll()
                    showCenteredMessage({ title: '短信设置已保存', message: '短信中心和存储位置已经写入基带。' })
                } catch (error) {
                    showError('保存短信设置失败', error)
                } finally {
                    button.disabled = false
                }
            })
            get('#u20SmsExport').addEventListener('click', () => {
                const exportData = messages.map(message => ({
                    id: message.id,
                    number: message.number,
                    time: formatDate(message.date),
                    status: tagInfo(message.tag)[0],
                    store: message.store || '',
                    content: decodeBody(message.content)
                }))
                const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json;charset=utf-8' })
                const link = document.createElement('a')
                link.href = URL.createObjectURL(blob)
                link.download = `U20-SMS-${new Date().toISOString().slice(0, 10)}.json`
                link.click()
                window.setTimeout(() => URL.revokeObjectURL(link.href), 1000)
            })

            await refreshAll()
        }

        async function renderUsbPage() {
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            inlineStatic.innerHTML = `
                <article class="u20-inline-page u20-usb-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>USB 模式</h2><p>只切换本次运行状态，重启后自动恢复原厂 charging + ADB</p></div>
                        <button type="button" id="u20UsbRefresh">刷新状态</button>
                    </div>
                    <div class="u20-usb-status">
                        <span><small>当前模式</small><strong id="u20UsbCurrent">读取中…</strong></span>
                        <span><small>USB 状态</small><strong id="u20UsbState">--</strong></span>
                        <span><small>重启默认</small><strong id="u20UsbDefault">charging,adb</strong></span>
                    </div>
                    <div class="u20-usb-options">
                        <button type="button" data-usb-mode="charging,adb">
                            <b>原厂模式</b><span>charging + ADB</span><small>供电与 ADB，不创建 USB 网卡</small>
                        </button>
                        <button type="button" data-usb-mode="rndis,adb">
                            <b>TetherKit 模式</b><span>RNDIS + ADB</span><small>创建 USB 网卡，同时保留 ADB</small>
                        </button>
                    </div>
                    <p class="u20-usb-note">切换时 USB 会短暂重连。如果当前通过 192.168.99.1 访问，切回原厂模式后页面会断开，可改用 Wi-Fi 地址 192.168.88.1:2333。</p>
                </article>`

            const current = inlineStatic.querySelector('#u20UsbCurrent')
            const usbState = inlineStatic.querySelector('#u20UsbState')
            const defaultMode = inlineStatic.querySelector('#u20UsbDefault')
            const modeButtons = [...inlineStatic.querySelectorAll('[data-usb-mode]')]

            async function loadUsbMode() {
                modeButtons.forEach(button => { button.disabled = true })
                current.textContent = '读取中…'
                try {
                    const response = await fetch(`${KANO_baseURL}/u20/usb_mode`, { headers: { ...common_headers } })
                    const data = await response.json()
                    if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                    current.textContent = data.current
                    usbState.textContent = data.state
                    defaultMode.textContent = data.default
                    modeButtons.forEach(button => {
                        button.disabled = false
                        button.classList.toggle('is-active', button.dataset.usbMode === data.current)
                    })
                } catch (error) {
                    current.textContent = '读取失败'
                    usbState.textContent = error.message
                    modeButtons.forEach(button => { button.disabled = false })
                }
            }

            async function switchUsbMode(mode) {
                const isRndis = mode === 'rndis,adb'
                const confirmed = await confirmCenteredDialog({
                    title: isRndis ? '开启 TetherKit 模式' : '恢复原厂 USB 模式',
                    message: isRndis
                        ? '将临时切换为 RNDIS + ADB，USB 会断开并重新连接。'
                        : '将切回 charging + ADB，TetherKit 创建的 USB 网卡会消失。',
                    confirmText: '确认切换'
                })
                if (!confirmed) return
                modeButtons.forEach(button => { button.disabled = true })
                try {
                    const response = await fetch(`${KANO_baseURL}/u20/usb_mode`, {
                        method: 'POST',
                        headers: { ...common_headers, 'Content-Type': 'application/json' },
                        body: JSON.stringify({ mode })
                    })
                    const data = await response.json()
                    if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                    showCenteredMessage({
                        title: 'USB 模式切换中',
                        message: '命令已发送，USB 将在 2 秒后重新连接。设备重启后仍恢复原厂模式。'
                    })
                    window.setTimeout(loadUsbMode, 5000)
                } catch (error) {
                    modeButtons.forEach(button => { button.disabled = false })
                    showCenteredMessage({ title: 'USB 模式切换失败', message: error.message, danger: true })
                }
            }

            inlineStatic.querySelector('#u20UsbRefresh').addEventListener('click', loadUsbMode)
            modeButtons.forEach(button => {
                button.addEventListener('click', () => switchUsbMode(button.dataset.usbMode))
            })
            await loadUsbMode()
        }

        function stopWifiClientRefresh() {
            if (!wifiClientRefreshTimer) return
            window.clearInterval(wifiClientRefreshTimer)
            wifiClientRefreshTimer = null
        }

        async function renderWifiClientsPage() {
            stopWifiClientRefresh()
            state.dockMode = false
            hideDockedFeatures()
            inlineStatic.hidden = false
            inlineHost.hidden = false
            inlineStatic.innerHTML = `
                <article class="u20-inline-page u20-wifi-clients-page">
                    <div class="u20-inline-page-heading">
                        <div><h2>Wi-Fi 接入设备</h2><p>直接读取 U20 无线驱动、邻居表与真实转发计数</p></div>
                        <button type="button" id="u20WifiClientRefresh">刷新</button>
                    </div>
                    <div class="u20-client-summary">
                        <span><small>在线客户端</small><strong id="u20ClientCount">读取中…</strong></span>
                        <span><small>Wi-Fi 网络</small><strong id="u20ClientSsid">--</strong></span>
                        <span><small>当前下载</small><strong id="u20ClientsDown">--</strong></span>
                        <span><small>当前上传</small><strong id="u20ClientsUp">--</strong></span>
                        <span><small>监控累计</small><strong id="u20ClientsTotal">--</strong></span>
                    </div>
                    <div class="u20-client-notice" id="u20ClientNotice">流量为设备经 U20 转发的真实计数；在线时长从本次监测到客户端开始计算。</div>
                    <section class="u20-client-list" id="u20ClientList"><p class="u20-sms-empty">正在读取客户端…</p></section>
                </article>`

            const root = inlineStatic.querySelector('.u20-wifi-clients-page')
            const list = root.querySelector('#u20ClientList')
            let loading = false

            function signalBars(client) {
                const percent = numberValue(client.signal_percent)
                const count = percent === null ? 0 : Math.max(1, Math.min(4, Math.ceil(percent / 25)))
                return `<span class="u20-client-signal" title="${client.rssi == null ? '信号未知' : `${client.rssi} dBm`}" aria-label="信号 ${count} 格">${[0, 1, 2, 3].map(index => `<i class="${index < count ? 'is-active' : ''}"></i>`).join('')}</span>`
            }

            function renderClients(data, force = false) {
                root.querySelector('#u20ClientCount').textContent = `${data.client_count || 0} / ${data.max_clients || '--'} 台`
                root.querySelector('#u20ClientSsid').textContent = `${safeValue(data.ssid)} · ${safeValue(data.band)} · CH ${safeValue(data.channel)}`
                root.querySelector('#u20ClientsDown').textContent = `↓ ${formatRate(data.download_rate)}`
                root.querySelector('#u20ClientsUp').textContent = `↑ ${formatRate(data.upload_rate)}`
                root.querySelector('#u20ClientsTotal').textContent = `↓ ${formatBytes(data.download_bytes)} · ↑ ${formatBytes(data.upload_bytes)}`
                const focused = root.contains(document.activeElement) && /INPUT|SELECT|TEXTAREA/.test(document.activeElement.tagName)
                if (focused && !force) return
                const clients = Array.isArray(data.clients) ? data.clients : []
                if (!clients.length) {
                    list.innerHTML = '<p class="u20-sms-empty">当前没有连接 Wi-Fi 的客户端</p>'
                    return
                }
                list.innerHTML = clients.map(client => {
                    const mac = escapeHtml(client.mac)
                    const maxRate = Math.max(1, Number(client.download_rate) || 0, Number(client.upload_rate) || 0)
                    const downWidth = Math.max(2, Math.round((Number(client.download_rate) || 0) * 100 / maxRate))
                    const upWidth = Math.max(2, Math.round((Number(client.upload_rate) || 0) * 100 / maxRate))
                    return `<article class="u20-client-card${client.blocked ? ' is-blocked' : ''}" data-client-mac="${mac}">
                        <header>
                            <div class="u20-client-identity">
                                <span class="u20-client-device-icon">${client.randomized_mac ? '◇' : '▣'}</span>
                                <div><strong>${escapeHtml(client.name)}</strong><small>${escapeHtml(client.ip || '等待 IPv4')} · ${mac}</small></div>
                            </div>
                            <div class="u20-client-badges">${client.randomized_mac ? '<span>随机 MAC</span>' : ''}<span class="${client.blocked ? 'is-danger' : 'is-online'}">${client.blocked ? '已禁止上网' : '在线'}</span>${signalBars(client)}</div>
                        </header>
                        <div class="u20-client-facts">
                            <span><small>信号强度</small><strong>${client.rssi == null ? '--' : `${client.rssi} dBm`}</strong></span>
                            <span><small>在线时长</small><strong>${formatDuration(client.connected_seconds)}</strong></span>
                            <span><small>无线链路</small><strong>${client.mcs == null ? '--' : `MCS ${client.mcs}`} ${client.spatial_streams == null ? '' : `· ${client.spatial_streams} 流`}</strong></span>
                            <span><small>发送失败</small><strong>${client.tx_failed == null ? '--' : client.tx_failed}</strong></span>
                            <span><small>累计下载</small><strong>${formatBytes(client.download_bytes)}</strong></span>
                            <span><small>累计上传</small><strong>${formatBytes(client.upload_bytes)}</strong></span>
                        </div>
                        <div class="u20-client-rate-row">
                            <span><b>↓ ${formatRate(client.download_rate)}</b><i style="--rate:${downWidth}%"></i></span>
                            <span><b>↑ ${formatRate(client.upload_rate)}</b><i style="--rate:${upWidth}%"></i></span>
                        </div>
                        <div class="u20-client-controls">
                            <label><span>设备名称</span><input type="text" maxlength="40" data-client-name value="${escapeHtml(client.custom_name || client.dhcp_name || '')}" placeholder="自定义名称"></label>
                            <button type="button" data-client-action="rename">保存名称</button>
                            <label><span>下载限速 Kbps</span><input type="number" min="0" max="1000000" data-client-download value="${Number(client.download_limit_kbps) || 0}" placeholder="0 不限速"></label>
                            <label><span>上传限速 Kbps</span><input type="number" min="0" max="1000000" data-client-upload value="${Number(client.upload_limit_kbps) || 0}" placeholder="0 不限速"></label>
                            <button type="button" data-client-action="set_limit">应用限速</button>
                            <button type="button" class="${client.blocked ? '' : 'is-danger'}" data-client-action="${client.blocked ? 'unblock' : 'block'}">${client.blocked ? '恢复上网' : '禁止上网'}</button>
                        </div>
                        ${Array.isArray(client.ipv6) && client.ipv6.length ? `<details><summary>IPv6 地址</summary><code>${client.ipv6.map(escapeHtml).join('<br>')}</code></details>` : ''}
                    </article>`
                }).join('')
            }

            async function loadClients(force = false) {
                if (loading || state.page !== 'wireless' || state.panel !== 'clients') return
                loading = true
                try {
                    const response = await fetch(`${KANO_baseURL}/u20/wifi/clients`, { headers: { ...common_headers } })
                    const data = await response.json()
                    if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                    renderClients(data, force)
                    document.getElementById('u20HeaderClients').textContent = `Wi-Fi ${data.client_count || 0}`
                } catch (error) {
                    if (!list.querySelector('.u20-client-card')) list.innerHTML = `<p class="u20-sms-empty">读取客户端失败：${escapeHtml(error.message)}</p>`
                } finally {
                    loading = false
                }
            }

            async function postClientAction(card, action) {
                const mac = card.dataset.clientMac
                const button = card.querySelector(`[data-client-action="${action}"]`)
                const payload = { action, mac }
                if (action === 'rename') payload.name = card.querySelector('[data-client-name]').value.trim()
                if (action === 'set_limit') {
                    payload.download_limit_kbps = Number(card.querySelector('[data-client-download]').value) || 0
                    payload.upload_limit_kbps = Number(card.querySelector('[data-client-upload]').value) || 0
                }
                if (action === 'block') {
                    const confirmed = await confirmCenteredDialog({
                        title: '禁止该设备上网',
                        message: `将禁止 ${card.querySelector('.u20-client-identity strong').textContent} 访问互联网，但仍允许打开设备管理页以便恢复。`,
                        confirmText: '确认禁止',
                        danger: true
                    })
                    if (!confirmed) return
                }
                button.disabled = true
                try {
                    const response = await fetch(`${KANO_baseURL}/u20/wifi/client/action`, {
                        method: 'POST',
                        headers: { ...common_headers, 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    })
                    const data = await response.json()
                    if (!response.ok || data.error) throw new Error(data.error || `HTTP ${response.status}`)
                    window.createToast && createToast('客户端设置已生效', 'green')
                    await loadClients(true)
                } catch (error) {
                    showCenteredMessage({ title: '设置失败', message: error.message, danger: true })
                    button.disabled = false
                }
            }

            list.addEventListener('click', event => {
                const button = event.target.closest('[data-client-action]')
                const card = button?.closest('[data-client-mac]')
                if (button && card) postClientAction(card, button.dataset.clientAction)
            })
            root.querySelector('#u20WifiClientRefresh').addEventListener('click', () => loadClients(true))
            await loadClients(true)
            wifiClientRefreshTimer = window.setInterval(() => loadClients(false), 2500)
        }

        function renderSubnav() {
            const page = pages[state.page]
            const subnav = subnavWrap.querySelector('.u20-subnav')
            document.getElementById('u20SectionLabel').textContent = page.title
            subnav.innerHTML = page.actions.map((action, index) => `
                <button type="button" data-action-index="${index}" class="${actionKey(action) === state.activeAction ? 'is-active' : ''}">${action.label}</button>
            `).join('')
            subnav.querySelectorAll('button').forEach(button => {
                button.addEventListener('click', () => runAction(page.actions[Number(button.dataset.actionIndex)]))
            })
        }

        function updatePageHeading(action) {
            const page = pages[state.page]
            document.getElementById('u20PageTitle').textContent = action?.label || page.title
            document.getElementById('u20PageDescription').textContent = action?.note || page.description
        }

        function renderActionPanel() {
            actionPanel.hidden = true
            actionPanel.innerHTML = ''
        }

        function updatePanels() {
            Object.values(panelMap).forEach(panel => panel && (panel.hidden = true))
            overviewGroups.forEach(group => { group.hidden = true })
            overviewPage.hidden = true
            summary.hidden = true
            signalDetails.hidden = true
            if (state.panel === 'overview') {
                overviewPage.hidden = false
                summary.hidden = false
                signalDetails.hidden = false
                overviewGroups.forEach(group => { group.hidden = false })
                ;[panelMap.monitor, panelMap.status].forEach(panel => {
                    if (!panel) return
                    panel.hidden = false
                    ensureOpen(panel)
                })
                return
            }
            const selected = panelMap[state.panel]
            if (selected) {
                if (selected === panelMap.monitor || selected === panelMap.status) {
                    overviewPage.hidden = false
                    summary.hidden = false
                    const group = selected === panelMap.monitor ? deviceOverviewGroup : otherOverviewGroup
                    group.hidden = false
                }
                selected.hidden = false
                ensureOpen(selected)
            }
        }

        function runAction(action) {
            if (!action) return
            if (action.panel !== 'clients') stopWifiClientRefresh()
            if (defaultActionTimer) {
                window.clearTimeout(defaultActionTimer)
                defaultActionTimer = null
            }
            if (!state.authenticated) {
                showModal('#tokenModal')
                scanForDockable()
                return
            }
            state.activeAction = actionKey(action)
            resetDashboardPosition(true)
            renderSubnav()
            updatePageHeading(action)
            if (action.panel) {
                state.panel = action.panel
                state.dockMode = false
                hideDockedFeatures()
                inlineHost.hidden = true
                updatePanels()
                if (action.panel === 'tools') {
                    summary.hidden = true
                    renderToolsPage()
                    return
                }
                if (action.panel === 'sms') {
                    summary.hidden = true
                    renderSmsPage()
                    return
                }
                if (action.panel === 'donate') {
                    summary.hidden = true
                    renderDonatePage()
                    return
                }
                if (action.panel === 'usb') {
                    summary.hidden = true
                    renderUsbPage()
                    return
                }
                if (action.panel === 'clients') {
                    summary.hidden = true
                    renderWifiClientsPage()
                    return
                }
                return
            }
            state.panel = ''
            updatePanels()
            summary.hidden = true
            const modalSelector = modalTargets[action.target]
            if (!modalSelector) {
                renderCommandPage(action)
                return
            }
            state.dockMode = true
            hideDockedFeatures()
            inlineStatic.hidden = true
            inlineStatic.innerHTML = ''
            inlineHost.hidden = false
            activateOriginal(action.target)
            ;[0, 80, 240, 700].forEach(delay => window.setTimeout(() => {
                const modal = document.querySelector(modalSelector)
                if (modal && modal.style.display !== 'none') dockFeature(modal)
                else scanForDockable()
            }, delay))
        }

        function selectPage(pageKey, openDefault = true) {
            if (!pages[pageKey]) pageKey = 'overview'
            stopWifiClientRefresh()
            if (defaultActionTimer) {
                window.clearTimeout(defaultActionTimer)
                defaultActionTimer = null
            }
            state.page = pageKey
            const firstAction = pages[pageKey].actions[0]
            state.panel = firstAction.panel || ''
            state.activeAction = actionKey(firstAction)
            state.dockMode = false
            resetDashboardPosition(true)
            hideDockedFeatures()
            inlineHost.hidden = true
            localStorage.setItem('u20_dashboard_page', pageKey)
            primary.querySelectorAll('button').forEach(button => {
                button.classList.toggle('is-active', button.dataset.page === pageKey)
            })
            renderSubnav()
            updatePageHeading(firstAction)
            renderActionPanel()
            updatePanels()
            if (openDefault && firstAction.target) {
                defaultActionTimer = window.setTimeout(() => {
                    defaultActionTimer = null
                    if (state.page === pageKey && state.activeAction === actionKey(firstAction)) {
                        runAction(firstAction)
                    }
                }, 350)
            } else if (firstAction.panel === 'tools') {
                renderToolsPage()
            } else if (firstAction.panel === 'sms') {
                renderSmsPage()
            } else if (firstAction.panel === 'clients') {
                renderWifiClientsPage()
            }
        }

        primary.querySelectorAll('button').forEach(button => {
            button.addEventListener('click', () => selectPage(button.dataset.page))
        })
        footer && footer.querySelector('#u20DonateLink').addEventListener('click', () => {
            selectPage('system', false)
            runAction(pages.system.actions.find(action => action.panel === 'donate'))
        })

        function setAuthState(loggedIn) {
            state.authenticated = Boolean(loggedIn)
            document.body.classList.toggle('u20-auth-locked', !state.authenticated)
            if (state.authenticated) {
                const tokenModal = document.getElementById('tokenModal')
                if (tokenModal) tokenModal.style.display = 'none'
                state.dockMode = false
                hideDockedFeatures()
                inlineHost.hidden = true
                selectPage(localStorage.getItem('u20_dashboard_page') || 'overview')
                resetDashboardPosition(true)
                syncCloudControl()
                syncWebEntry()
                return
            }
            Object.values(panelMap).forEach(panel => panel && (panel.hidden = true))
            overviewPage.hidden = true
            summary.hidden = true
            actionPanel.hidden = true
            state.dockMode = true
            ;[0, 100, 360].forEach(delay => window.setTimeout(scanForDockable, delay))
        }

        window.addEventListener('ufi-auth-changed', event => {
            setAuthState(Boolean(event.detail?.loggedIn))
        })

        const trafficHistory = Array(18).fill(0)
        let lastNetworkSample = null
        let lastSignalSnapshot = ''
        let lastSignalUpdate = null

        function updateTrafficVisual(rxRate, txRate, sampleKey, wifiRxRate, wifiTxRate) {
            const rx = Math.max(0, numberValue(rxRate) || 0)
            const tx = Math.max(0, numberValue(txRate) || 0)
            const total = rx + tx
            const wifiTotal = Math.max(0, numberValue(wifiRxRate) || 0) + Math.max(0, numberValue(wifiTxRate) || 0)
            const normalized = Math.max(0, Math.min(1, Math.log1p(wifiTotal) / Math.log1p(10 * 1024 * 1024)))

            if (sampleKey !== lastNetworkSample) {
                lastNetworkSample = sampleKey
                trafficHistory.push(total)
                trafficHistory.shift()
                document.querySelectorAll('#u20TrafficWave i').forEach((bar, index) => {
                    const sample = trafficHistory[index] || 0
                    const level = Math.max(0, Math.min(1, Math.log1p(sample) / Math.log1p(10 * 1024 * 1024)))
                    bar.style.height = `${2 + level * 26}px`
                    bar.classList.toggle('is-active', sample > 0)
                })
            }

            const radar = document.getElementById('u20WifiRadar')
            if (radar) {
                radar.style.setProperty('--wifi-activity', normalized.toFixed(3))
                radar.querySelectorAll('i').forEach((ring, index) => {
                    const ringLevel = Math.max(0, normalized - index * .08)
                    ring.style.opacity = `${.1 + ringLevel * .75}`
                    ring.style.transform = `scale(${.82 + ringLevel * .18})`
                })
                radar.classList.toggle('has-traffic', wifiTotal > 0)
            }
            document.getElementById('u20WifiPulse')?.classList.toggle('is-active', wifiTotal > 0)
        }

        function updateSignalVisual(data, isOnline) {
            const quality = signalQuality(data)
            const barCount = isOnline && numberValue(signalValue(data)) !== null
                ? Math.max(1, Math.min(5, Math.ceil(quality / 20)))
                : 0
            const bars = document.getElementById('u20RadioBars')
            bars?.querySelectorAll('span').forEach((bar, index) => {
                bar.classList.toggle('is-active', index < barCount)
            })
            if (bars) bars.setAttribute('aria-label', `信号 ${barCount} 格`)

            const setFactQuality = (id, value, worst, best) => {
                const score = metricQuality(value, worst, best)
                const card = document.getElementById(id)
                if (!card) return
                card.style.setProperty('--fact-quality', `${score === null ? 0 : score}%`)
                card.classList.toggle('has-value', score !== null)
            }
            setFactQuality('u20FactRsrpCard', signalValue(data), -120, -80)
            setFactQuality('u20FactRsrqCard', data.nr_rsrq || data.lte_rsrq, -20, -8)
            setFactQuality('u20FactSinrCard', data.Nr_snr || data.Lte_snr, -10, 20)

            return quality
        }

        function updateMetrics() {
            const data = window.UFI_DATA || {}
            const network = networkLabel(data)
            const carrier = safeValue(data.network_provider, '未识别运营商')
            const signal = signalValue(data)
            const cpu = percentage(data.cpu_usage)
            const memory = percentage(data.mem_usage)
            const temperature = numberValue(data.cpu_temp)
            const temperatureText = temperature === null ? '--°C' : `${(temperature > 1000 ? temperature / 1000 : temperature).toFixed(1)}°C`
            const simState = simStatusLabel(data.sim_status)
            const rsrq = safeValue(data.nr_rsrq || data.lte_rsrq)
            const sinr = safeValue(data.Nr_snr || data.Lte_snr)
            const pci = safeValue(data.Nr_pci || data.Lte_pci)
            const isOnline = data.ppp_status === 'ppp_connected' || network !== '未注册'
            const signalPercent = updateSignalVisual(data, isOnline)
            const rxRate = numberValue(data.realtime_rx_thrpt) || 0
            const txRate = numberValue(data.realtime_tx_thrpt) || 0
            const sampleKey = safeValue(data.network_sample_time, `${rxRate}:${txRate}`)
            updateTrafficVisual(
                rxRate,
                txRate,
                sampleKey,
                data.wifi_realtime_rx_thrpt,
                data.wifi_realtime_tx_thrpt
            )
            const signalSnapshot = [sampleKey, signal, rsrq, sinr, pci, activeBand(data)].join('|')
            if (signalSnapshot !== lastSignalSnapshot) {
                lastSignalSnapshot = signalSnapshot
                lastSignalUpdate = new Date()
            }
            const headerNetwork = document.getElementById('u20HeaderNetwork')
            headerNetwork.lastChild.textContent = network
            headerNetwork.classList.toggle('is-online', isOnline)
            document.getElementById('u20HeaderCarrier').textContent = carrier
            document.getElementById('u20HeaderClients').textContent = `Wi-Fi ${safeValue(data.wifi_access_sta_num, '0')}`
            document.getElementById('u20CellularState').textContent = isOnline ? `${network} 已连接` : '等待网络'

            document.getElementById('u20MetricNetwork').textContent = `${network} · ${carrier}`
            document.getElementById('u20MetricNetworkMeta').textContent = `${activeBand(data)} · ${isOnline ? '数据连接正常' : '等待网络注册'}`
            document.getElementById('u20MetricSignal').textContent = signal === '--' ? '--' : `${signal} dBm`
            document.getElementById('u20MetricSignalMeta').textContent = `SINR ${sinr} · RSRQ ${rsrq}`
            document.getElementById('u20SignalGauge').style.setProperty('--gauge', `${signalPercent * 3.6}deg`)
            document.getElementById('u20SignalQuality').textContent = signalPercent
            document.getElementById('u20SimBadge').textContent = simState
            document.getElementById('u20SignalState').textContent = lastSignalUpdate
                ? `实时 ${lastSignalUpdate.toLocaleTimeString('zh-CN', { hour12: false })}`
                : simState
            document.getElementById('u20CpuValue').textContent = `${numberValue(data.cpu_usage) === null ? '--' : cpu.toFixed(0)}%`
            document.getElementById('u20MemValue').textContent = `${numberValue(data.mem_usage) === null ? '--' : memory.toFixed(0)}%`
            document.getElementById('u20CpuBar').style.width = `${cpu}%`
            document.getElementById('u20MemBar').style.width = `${memory}%`
            document.getElementById('u20ResourceTemp').textContent = temperatureText
            document.getElementById('u20MetricResourceMeta').textContent = `${safeValue(data.model, 'FM_U20')} · ${temperatureText}`
            document.getElementById('u20MetricClients').textContent = `${safeValue(data.wifi_access_sta_num, '0')} 台设备`
            document.getElementById('u20MetricClientsMeta').textContent = `接口 ${safeValue(data.network_interface, '--')} · 网关 ${safeValue(data.lan_ipaddr, '192.168.88.1')}`
            document.getElementById('u20WifiClients').textContent = `${safeValue(data.wifi_access_sta_num, '0')} 台设备`
            document.getElementById('u20WifiGateway').textContent = `网关 ${safeValue(data.lan_ipaddr, '192.168.88.1')}`
            document.getElementById('u20WifiAdminAddress').textContent = safeValue(data.lan_ipaddr, '192.168.88.1')
            document.getElementById('u20DownSpeed').textContent = formatRate(rxRate)
            document.getElementById('u20UpSpeed').textContent = formatRate(txRate)
            document.getElementById('u20FactRsrp').textContent = signal
            document.getElementById('u20FactRsrq').textContent = rsrq
            document.getElementById('u20FactSinr').textContent = sinr
            document.getElementById('u20FactBand').textContent = activeBand(data)
            document.getElementById('u20FactPci').textContent = pci
            document.getElementById('u20FactPpp').textContent = data.ppp_status === 'ppp_connected' ? '已连接' : '未连接'
            overviewPage.classList.toggle('has-network', isOnline)
        }

        selectPage(localStorage.getItem('u20_dashboard_page') || 'overview')
        setAuthState(window.UFI_AUTHENTICATED)
        updateMetrics()
        window.setInterval(updateMetrics, 1000)
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, { once: true })
    } else {
        initialize()
    }
})()
