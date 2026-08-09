/*
 * Just German — учебный проект
 * Copyright (c) 2026 chicagoist
 *
 * SPDX-License-Identifier: MIT
 *
 * Аудио, учебник и метод Assimil принадлежат правообладателю Assimil SAS
 * (Франция, assimil.com). Распространение этих материалов запрещено.
 * См. файл NOTICE.
 */

package com.chicagoist.justgerman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chicagoist.justgerman.ui.theme.JustGermanTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustGermanTheme {
                JustGermanApp()
            }
        }
    }
}
