package com.darknote.desktop.editor.theme

import androidx.compose.ui.graphics.Color

/**
 * Color scheme for syntax highlighting.
 * Based on popular code editor themes (IntelliJ IDEA Darcula, VS Code Dark+).
 */
data class EditorColorScheme(
    val keyword: Color,
    val type: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val annotation: Color,
    val function: Color,
    val operator: Color,
    val constant: Color,
    val variable: Color
) {
    companion object {
        /**
         * Default dark theme colors (IntelliJ IDEA Darcula inspired).
         */
        val DARK = EditorColorScheme(
            keyword = Color(0xFFCC7832),      // Orange
            type = Color(0xFFB389C5),         // Purple
            string = Color(0xFF6A8759),       // Green
            number = Color(0xFF6897BB),       // Cyan
            comment = Color(0xFF808080),      // Gray
            annotation = Color(0xFFBBB529),   // Yellow
            function = Color(0xFFFFC66D),     // Light orange
            operator = Color(0xFFA9B7C6),     // Light gray
            constant = Color(0xFF9876AA),     // Light purple
            variable = Color(0xFFA9B7C6)      // Light gray
        )
        
        /**
         * Light theme colors (IntelliJ IDEA Light inspired).
         */
        val LIGHT = EditorColorScheme(
            keyword = Color(0xFF0000FF),      // Blue
            type = Color(0xFF000080),         // Navy
            string = Color(0xFF008000),       // Green
            number = Color(0xFF0000FF),       // Blue
            comment = Color(0xFF808080),      // Gray
            annotation = Color(0xFF808000),   // Olive
            function = Color(0xFF795E26),     // Brown
            operator = Color(0xFF000000),     // Black
            constant = Color(0xFF660E7A),     // Purple
            variable = Color(0xFF000000)      // Black
        )
        
        /**
         * VS Code Dark+ inspired theme.
         */
        val VS_CODE_DARK = EditorColorScheme(
            keyword = Color(0xFFC586C0),      // Pink
            type = Color(0xFF4EC9B0),         // Teal
            string = Color(0xFFCE9178),       // Orange-red
            number = Color(0xFFB5CEA8),       // Light green
            comment = Color(0xFF6A9955),      // Green
            annotation = Color(0xFF569CD6),   // Blue
            function = Color(0xFFDCDCAA),     // Yellow
            operator = Color(0xFFD4D4D4),     // Light gray
            constant = Color(0xFF4FC1FF),     // Sky blue
            variable = Color(0xFF9CDCFE)      // Light blue
        )
    }
}
