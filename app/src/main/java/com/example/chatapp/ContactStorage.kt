package com.example.chatapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ContactStorage {

    private const val PREF_NAME = "chatapp_contacts"

    fun saveContactsForUser(context: Context, uid: String, contacts: List<Users>) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(contacts)
        editor.putString("contacts_$uid", json)
        editor.apply()
    }

    fun loadContactsForUser(context: Context, uid: String): ArrayList<Users> {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("contacts_$uid", null)
        val gson = Gson()
        val type = object : TypeToken<ArrayList<Users>>() {}.type
        return if (json != null) gson.fromJson(json, type) else arrayListOf()
    }
}