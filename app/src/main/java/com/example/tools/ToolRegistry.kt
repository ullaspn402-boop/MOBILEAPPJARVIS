package com.aistudio.jarvis.voiceagent.tools

object ToolRegistry {
    val allTools: List<JarvisTool> = listOf(
        AppLauncherTool(),
        CallContactTool(),
        MessagingTool(),
        AlarmTool(),
        ReminderTool(),
        CalendarTool(),
        MapsNavigationTool(),
        NotesTool(),
        MemoryTool(),
        NotificationTool(),
        DeviceSettingsTool(),
        WebSearchTool(),
        MusicTool(),
        SystemControlTool()
    )

    private val toolMap: Map<String, JarvisTool> = allTools.associateBy { it.id }

    fun getTool(id: String): JarvisTool? = toolMap[id]

    fun findToolsByCategory(category: String): List<JarvisTool> =
        allTools.filter { it.category.contains(category, ignoreCase = true) }
}
