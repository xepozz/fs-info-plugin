package com.github.xepozz.fs_info.actions

import com.github.xepozz.fs_info.FsInfoSettings
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import java.util.function.Function

class ToggleBytesAction : DumbAware, ToggleOptionAction(Function {
    object : Option {
        private val settings = it.project?.service<FsInfoSettings>()

        override fun isSelected() = settings?.showBytes ?: false

        override fun setSelected(selected: Boolean) {
            val updated = selected != isSelected

            settings?.showBytes = selected

            if (updated) {
                it.project?.let { project ->
                    ProjectView
                        .getInstance(project)
                        .currentProjectViewPane
                        ?.updateFromRoot(true)
                }
            }
        }
    }
})
