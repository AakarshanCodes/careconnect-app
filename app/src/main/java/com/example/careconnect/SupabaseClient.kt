package com.example.careconnect

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://ftzugnhwudupxsgkjcd.supabase.co",
    supabaseKey = "sb_publishable_ghgPgLKte8MZT3Q7zbWdlw_GL_G1CU2"
) {
    install(Postgrest)
}
