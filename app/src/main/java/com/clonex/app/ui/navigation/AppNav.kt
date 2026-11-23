package com.clonex.app.ui.navigation

/**
 * Rute navigasi utama CloneX.
 *
 * Tiga layar utama, tanpa layar palsu:
 * - Dashboard: daftar aplikasi main dan kerja dalam satu tempat.
 * - Kompatibilitas: status dukungan perangkat yang sebenarnya.
 * - Profil Kerja: provisioning resmi dan status Profile Owner.
 */
sealed class AppRoute(val route: String, val label: String) {
    object Dashboard : AppRoute("dashboard", "Beranda")
    object Compatibility : AppRoute("compatibility", "Kompatibilitas")
    object Provisioning : AppRoute("provisioning", "Profil Kerja")

    companion object {
        val all = listOf(Dashboard, Compatibility, Provisioning)
    }
}
