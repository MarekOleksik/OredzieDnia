package com.example.oredziednia

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Źródło danych dla orędzi - wydzielone z [MainViewModel] jako interfejs,
 * aby testy jednostkowe mogły podstawić własną (nie-sieciową) implementację.
 */
interface ApparitionRepository {
    suspend fun getAll(): List<Apparition>
}

class SupabaseApparitionRepository(
    private val client: SupabaseClient = supabase
) : ApparitionRepository {
    override suspend fun getAll(): List<Apparition> =
        client.from("apparitions").select().decodeList()
}
