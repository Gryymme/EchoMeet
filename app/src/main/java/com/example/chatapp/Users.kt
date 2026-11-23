package com.example.chatapp

class Users {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var lastSeen: Long = 0L
    var online: Boolean = false

    @Transient
    var userOnlineStatus: Boolean = false

    constructor(){}

    constructor(name: String?, email: String?, uid: String?) {
        this.name = name
        this.email = email
        this.uid = uid
    }

    constructor(name: String?, email: String?, uid: String?, lastSeen: Long, online: Boolean) {
        this.name = name
        this.email = email
        this.uid = uid
        this.lastSeen = lastSeen
        this.online = online
    }

}