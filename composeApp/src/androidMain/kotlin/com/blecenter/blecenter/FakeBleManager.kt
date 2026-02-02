package com.blecenter.blecenter

class FakeBleManager : BleManager(null) {
    override fun toString(): String {
        return "FakeBleManager"
    }
}