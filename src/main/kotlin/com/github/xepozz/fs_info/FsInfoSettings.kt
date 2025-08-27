package com.github.xepozz.fs_info

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "FsInfoSettings", storages = [Storage("fs_info.xml")])
class FsInfoSettings : BaseState(), PersistentStateComponent<FsInfoSettings> {
    var enabled by property(true)
    var showBytes by property(false)
    var showKBytes by property(true)
    var showMBytes by property(true)
    var showGBytes by property(true)
    var showLines by property(true)
    var showDirectoryItemsAmount by property(true)
    var showDirectorySize by property(true)

    override fun getState() = this
    override fun loadState(state: FsInfoSettings) = copyFrom(state)
}