import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  const { userId, pushToken } = await req.json()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  )

  const { error } = await supabase
    .from("profiles")
    .update({ push_token: pushToken, push_token_updated_at: new Date().toISOString() })
    .eq("id", userId)

  return new Response(
    JSON.stringify({ success: !error, error: error?.message }),
    { status: error ? 500 : 200 }
  )
})
