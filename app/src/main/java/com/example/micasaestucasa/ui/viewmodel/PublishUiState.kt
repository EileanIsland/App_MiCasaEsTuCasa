package com.example.micasaestucasa.ui.viewmodel

import com.example.micasaestucasa.data.model.Disponibilita
import java.util.Collections.emptyList

data class PublishUiState(
    val isFormValid: Boolean = false,
    val houseId: String = "",

    val isLoading: Boolean = false,
    val titolo: String = "",
    val tipologia: String = "",
    val descrizione: String = "",
    val prezzoNotte: Double = 0.0,
    val citta : String ="",
    val indirizzo: String = "",
    val latitudine: Double = 0.0,
    val longitudine: Double = 0.0,
    val ospitiMassimi: Int = 1,
    val numeroStanze: Int = 1,
    val numeroLetti: Int = 1,
    val numeroBagni: Int = 1,

    val images : List<String> = emptyList(),
    val esperienza : List<String> = emptyList(),
    val servizi : List<String> = emptyList(),
    val regole : List<String> = emptyList(),
    val disponibilita : List<Disponibilita> = emptyList(),

    val availableService: List<String> = emptyList(),
    val availableExperience: List<String> = emptyList(),
    val availableRule: List<String> = emptyList(),
    val availableCategory : List<String> = emptyList(),

    val errorMesasge: String? = null,
    val isSuccess : Boolean = false
)



