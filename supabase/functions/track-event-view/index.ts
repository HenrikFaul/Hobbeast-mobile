import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const { eventId, userId, source } = await req.json()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  )

  // Upsert into event_views (one row per user per event)
  await supabase.from("event_views").upsert({
    event_id: eventId,
    user_id: userId,
    source: source ?? "organic",
    last_viewed_at: new Date().toISOString(),
  }, { onConflict: "event_id,user_id" })

  // Refresh materialized analytics view
  await supabase.rpc("refresh_event_analytics", { p_event_id: eventId })

  return new Response(JSON.stringify({ ok: true }), { status: 200 })
})
