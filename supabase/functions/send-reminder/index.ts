import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
)

serve(async (req) => {
  const { eventId, hoursBeforeEvent = 24 } = await req.json()

  // Get attendees who are GOING and have push tokens
  const { data: attendees } = await supabase
    .from("attendees")
    .select("user_id, profiles!inner(push_token, display_name)")
    .eq("event_id", eventId)
    .eq("state", "going")

  const { data: event } = await supabase
    .from("events")
    .select("title, start_time, location")
    .eq("id", eventId)
    .single()

  if (!event || !attendees?.length) {
    return new Response(JSON.stringify({ sent: 0 }), { status: 200 })
  }

  const tokens = attendees
    .map((a: any) => a.profiles?.push_token)
    .filter(Boolean)

  // Send via FCM (batch)
  const fcmResponse = await fetch("https://fcm.googleapis.com/v1/projects/HOBBEAST/messages:send", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${Deno.env.get("FCM_ACCESS_TOKEN")}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        notification: {
          title: `🎉 ${event.title}`,
          body: `${hoursBeforeEvent === 1 ? "1 óra múlva" : hoursBeforeEvent + " óra múlva"} kezdődik!`,
        },
        data: {
          type: "reminder",
          event_id: eventId,
          navigate_to: `event/${eventId}`,
        },
        tokens,
      },
    }),
  })

  return new Response(JSON.stringify({ sent: tokens.length }), { status: 200 })
})
