package dev.marufeuille.hydra.sync

object HydrationSync {
    const val SIP_PATH_PREFIX = "/hydration/sip"
    const val STATUS_PATH = "/hydration/status"
    const val REQUEST_PATH = "/hydration/request"

    const val KEY_ID = "id"
    const val KEY_VOLUME_ML = "volume_ml"
    const val KEY_RECORDED_AT = "recorded_at"
    const val KEY_TODAY_ML = "today_ml"
    const val KEY_PERMITTED = "permitted"
    const val KEY_AVAILABLE = "available"
    const val KEY_UPDATED_AT = "updated_at"
    const val KEY_NUTRITION_PERMITTED = "nutrition_permitted"
    const val KEY_PROTEIN_G = "protein_g"
    const val KEY_FAT_G = "fat_g"
    const val KEY_CARBS_G = "carbs_g"
    const val KEY_KCAL = "kcal"
}
