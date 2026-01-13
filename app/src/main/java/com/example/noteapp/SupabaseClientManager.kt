package com.example.noteapp

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton object to manage Supabase client instance
 */
object SupabaseClientManager {
    
    private const val SUPABASE_URL = "https://brlboqfrckutnguhekur.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJybGJvcWZyY2t1dG5ndWhla3VyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgyNjk2OTEsImV4cCI6MjA4Mzg0NTY5MX0.54PZNpaGEArB2XTacpbIdGFjiBFEMADPYcDSVVtyYgo"
    
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
