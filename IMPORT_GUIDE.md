# Android Studio importálási útmutató

## 1. lépés – Gradle Wrapper jar letöltése

A gradle-wrapper.jar **nem szerepel** a projektben (bináris fájl).
Importálás előtt töltsd le:

```bash
# A projekt mappájában:
curl -L https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar \
     -o gradle/wrapper/gradle-wrapper.jar
```

**Vagy:** Android Studio automatikusan letölti, ha a gradle-wrapper.properties fájl megvan.

## 2. lépés – API kulcsok

Szerkeszd meg a `local.properties` fájlt (a projekt gyökerében):
- Töltsd ki legalább a `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `MAPS_API_KEY` és `GEOAPIFY_API_KEY` értékeket
- Részletes útmutató: lásd `hobbeast-docs/API_KEYS_GUIDE.md`

## 3. lépés – Importálás Android Studioba

1. Android Studio → **File → Open...**
2. Navigálj a `hobbeast-android/` mappába
3. Kattints **OK**-ra
4. Várj, amíg a Gradle sync lefut (első alkalommal ~5 perc, letölti a függőségeket)

## 4. lépés – Supabase séma

1. Nyisd meg a Supabase Dashboard-ot
2. SQL Editor → New query
3. Másold be és futtasd a `supabase/migrations/001_initial_schema.sql` tartalmát

## 5. lépés – Build és futtatás

```bash
./gradlew assembleDebug          # APK build
./gradlew installDebug           # Telepítés csatlakoztatott eszközre
./gradlew test                   # Unit tesztek
./gradlew connectedAndroidTest   # UI tesztek (eszköz szükséges)
```

## Minimális konfiguráció (gyors teszteléshez)

Ha csak ki szeretnéd próbálni az alkalmazást Supabase nélkül:
- A Supabase hívások hibát dobnak, de az alkalmazás elindítható
- A Room cache és a DataStore működik
- A UI teljesen navigálható
