package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Gestionnaire permettant de masquer ou afficher l'icône de l'application
 * sur l'écran d'accueil et le tiroir d'applications Android en activant/désactivant
 * le composant LauncherAlias déclaré dans le manifeste.
 */
object AppIconManager {

    private const val TAG = "AppIconManager"
    private const val LAUNCHER_ALIAS_CLASS = "com.example.LauncherAlias"

    /**
     * Vérifie si l'icône du lanceur est actuellement masquée.
     */
    fun isIconHidden(context: Context): Boolean {
        return try {
            val aliasComponent = ComponentName(context.packageName, LAUNCHER_ALIAS_CLASS)
            val state = context.packageManager.getComponentEnabledSetting(aliasComponent)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification visibilité icône: ${e.message}")
            false
        }
    }

    /**
     * Modifie la visibilité de l'icône du lanceur d'application.
     * @param visible true pour afficher l'icône, false pour la masquer.
     */
    fun setIconVisibility(context: Context, visible: Boolean) {
        try {
            val aliasComponent = ComponentName(context.packageName, LAUNCHER_ALIAS_CLASS)
            val newState = if (visible) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            context.packageManager.setComponentEnabledSetting(
                aliasComponent,
                newState,
                PackageManager.DONT_KILL_APP
            )
            Log.i(TAG, "Visibilité de l'icône mise à jour: visible=$visible (newState=$newState)")
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de modifier la visibilité de l'icône: ${e.message}", e)
        }
    }
}
