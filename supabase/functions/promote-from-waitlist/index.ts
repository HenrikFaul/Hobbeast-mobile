import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

// Triggered by a Postgres trigger when an attendee cancels (state → 'none')
serve(async (req) => {
  const { eventId } = await req.json()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  )

  const { data: event } = await supabase
    .from("events")
    .select("max_capacity, attendee_count")
    .eq("id", eventId)
    .single()

  if (!event?.max_capacity) return new Response(JSON.stringify({ promoted: 0 }))

  if (event.attendee_count < event.max_capacity) {
    // Promote oldest waitlisted attendee
    const { data: waitlisted } = await supabase
      .from("attendees")
      .select("id, user_id, profiles!inner(push_token, display_name)")
      .eq("event_id", eventId)
      .eq("state", "waitlisted")
      .order("joined_at", { ascending: true })
      .limit(1)

    if (waitlisted?.length) {
      const attendee = waitlisted[0]
      await supabase
        .from("attendees")
        .update({ state: "going" })
        .eq("id", attendee.id)

      // Notify the promoted user
      const pushToken = (attendee as any).profiles?.push_token
      if (pushToken) {
        await fetch("https://fcm.googleapis.com/v1/projects/HOBBEAST/messages:send", {
          method: "POST",
          headers: { "Authorization": `Bearer ${Deno.env.get("FCM_ACCESS_TOKEN")}` },
          body: JSON.stringify({
            message: {
              token: pushToken,
              notification: { title: "Jó híred van! 🎉", body: "Lekerültél a várólistáról – most már biztosan részt vehetsz!" },
              data: { type: "waitlist_promoted", event_id: eventId, navigate_to: `event/${eventId}` },
            },
          }),
        })
      }
      return new Response(JSON.stringify({ promoted: 1 }))
    }
  }

  return new Response(JSON.stringify({ promoted: 0 }))
})
