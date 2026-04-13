package com.darknote.sync

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.v2.DbxClientV2
import java.io.File
import java.util.Properties

class DropboxClient(private val appKey: String) {
    private val config = DbxRequestConfig.newBuilder("darknote/1.0").build()
    private val credentialsPath = File(System.getProperty("user.home"), ".config/darknote/credentials.properties")
    private var client: DbxClientV2? = null
    private var pkceWebAuth: DbxPKCEWebAuth? = null

    init {
        credentialsPath.parentFile.mkdirs()
        loadClient()
    }

    /**
     * Genera la URL de autorización para que el usuario la apruebe en el navegador.
     */
    fun getAuthUrl(): String {
        val appInfo = DbxAppInfo(appKey)
        pkceWebAuth = DbxPKCEWebAuth(config, appInfo)
        val authRequest = DbxWebAuth.newRequestBuilder()
            .withNoRedirect()
            .withTokenAccessType(com.dropbox.core.TokenAccessType.OFFLINE) // Para obtener refresh_token
            .build()
        return pkceWebAuth!!.authorize(authRequest)
    }

    /**
     * Finaliza la autenticación usando el código proporcionado por el usuario.
     */
    fun finishAuth(code: String) {
        val authFinish: DbxAuthFinish = pkceWebAuth!!.finishFromCode(code)
        saveTokens(authFinish.accessToken, authFinish.refreshToken)
        loadClient()
        println("Autenticación completada con éxito.")
    }

    private fun saveTokens(accessToken: String, refreshToken: String?) {
        val props = Properties()
        props.setProperty("access_token", accessToken)
        if (refreshToken != null) {
            props.setProperty("refresh_token", refreshToken)
        }
        props.store(credentialsPath.outputStream(), "Darknote Dropbox Credentials")
    }

    private fun loadClient() {
        if (credentialsPath.exists()) {
            val props = Properties()
            props.load(credentialsPath.inputStream())
            val accessToken = props.getProperty("access_token")
            val refreshToken = props.getProperty("refresh_token")
            
            if (refreshToken != null) {
                // Usar refresh token para inicializar el cliente (auto-renovación)
                client = DbxClientV2(config, refreshToken, appKey)
            } else if (accessToken != null) {
                client = DbxClientV2(config, accessToken)
            }
        }
    }

    fun isAuthorized(): Boolean = client != null

    fun getClient(): DbxClientV2? = client
}
