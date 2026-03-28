import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

function generateCode(length = 8): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join("")
}

serve(async (req) => {
  const { eventId } = await req.json()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  )

  // Assign unique codes to all attendees without one
  const { data: attendees } = await supabase
    .from("attendees")
    .select("id")
    .eq("event_id", eventId)
    .is("invite_code", null)

  if (!attendees?.length) return new Response(JSON.stringify({ updated: 0 }))

  for (const attendee of attendees) {
    const code = `HOB-${generateCode(6)}`
    await supabase.from("attendees").update({ invite_code: code }).eq("id", attendee.id)
  }

  return new Response(JSON.stringify({ updated: attendees.length }))
})
