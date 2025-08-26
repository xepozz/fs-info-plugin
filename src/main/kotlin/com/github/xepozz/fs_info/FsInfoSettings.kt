package com.github.xepozz.fs_info

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@Service(Service.Level.PROJECT)
@State(name = "FsInfoSettings", storages = [Storage("fs_info.xml")])
class FsInfoSettings : BaseState(), PersistentStateComponent<FsInfoSettings> {
    var showBytes by property(false)
    var showKBytes by property(true)
    var showMBytes by property(true)
    var showGBytes by property(true)
    var showLines by property(true)
    var showDirectoryItems by property(true)

    override fun getState() = this
    override fun loadState(state: FsInfoSettings) = copyFrom(state)
}