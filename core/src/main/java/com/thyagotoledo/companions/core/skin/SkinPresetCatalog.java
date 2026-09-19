package com.thyagotoledo.companions.core.skin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catalogo de presets de skins populares (animes e personagens conhecidos)
 * contendo texturas validas codificadas em Base64 no padrao oficial da Mojang.
 * Compilavel em Java 8 para compatibilidade universal entre plataformas.
 */
public class SkinPresetCatalog {

    public static final class PresetSkin {
        private final String name;
        private final String textureUrl;
        private final boolean slim;
        private final String base64Value;
        private final String signature;
        private final String profileId;

        public PresetSkin(String name, String textureUrl, boolean slim, String base64Value, String signature, String profileId) {
            this.name = name;
            this.textureUrl = textureUrl;
            this.slim = slim;
            this.base64Value = base64Value;
            this.signature = signature;
            this.profileId = profileId;
        }

        public String getName() {
            return name;
        }

        public String getTextureUrl() {
            return textureUrl;
        }

        public boolean isSlim() {
            return slim;
        }

        public String getBase64Value() {
            return base64Value;
        }

        public String getSignature() {
            return signature;
        }

        public String getProfileId() {
            return profileId;
        }

        public boolean hasSignature() {
            return signature != null && !signature.isEmpty();
        }
    }

    private static final Map<String, PresetSkin> PRESETS = new HashMap<>();

    static {
        registerSigned("Rimuru", "http://textures.minecraft.net/texture/1f36749cba76c983599a1514b0854092c30cb9c79755c9607a4077ee9cf8f76e", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NDkyNywKICAicHJvZmlsZUlkIiA6ICI1MThlNzgyODIyOTc0ODk0YWYyMTJkODhhNWMwMTJjZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSaW11cnUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWYzNjc0OWNiYTc2Yzk4MzU5OWExNTE0YjA4NTQwOTJjMzBjYjljNzk3NTVjOTYwN2E0MDc3ZWU5Y2Y4Zjc2ZSIKICAgIH0KICB9Cn0=",
                "Pux+msRDc1bf4jVSuA7QCiqZKZHJslmSxZtTvrjiAqWMLvQX4qug7K7JEzFTXYYVYMphz2AeKvuBFOfdKfFAPdYQPR87iphtSwvIXt8nKKh2hol3AoZ81fi10udhvPDJiljjEqWK8rxo3UO3cXdnodthRZHNUc7Poragwdg8pIg42NoA7R+h5AiRzUvKw8Vk3kPMl5S0hwooV6qSCNIlomclyJx6ZvcWPjsZl08xrwXBPaQgRtTXqCJFD/js1rAE3WiX7dgu7PIPYbzGmKigJvzuJFlpCzjZdy5fYNNkC89SWR/qS3KlcO3ycrNE2LsS48c4gm1jZtyA4XyVChYnYxVhhYbNE9bgWKBOyKk9lGOECjSefyBm3mPdwUeN9yHgjvY0uK0WOZtyUrU/VdFy9MgiZoAmNeAxXq6UW5wQCThq5iTbePNl3qtErpHAJEqGJr1VQT7T5a6CLEa1pJBs09LIc7EjMkEgVH8gpShAJ2YY9ztdt0KQnX1irt5CJ95WOQLkGxF6Mrfyfgcm3sQK16Mh5EAjI6sHE2oRN+STfbdeoh4tbJYSIMIbK8XpFJUrxtVs0VsFiRw+I/i9XEqD6o7zQoxkQ7xO0q9+mtxUGnFev+ZwWndZvysTqOzakPqfum9P0dSTO0rFWSm/YTZwLxQY3fkRBDkS9lYOU3tcTXA=",
                "518e782822974894af212d88a5c012cf");

        registerSigned("Goku", "http://textures.minecraft.net/texture/b43f45dba4e09732d42eaf34a7f53a69182281fd26f7850c4383a59ea6e875f5", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NTYyNiwKICAicHJvZmlsZUlkIiA6ICIzOTI5ZTA1MmJhZDc0Yjc0ODE3NzRkNWJhNTkxZmJkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJHb2t1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2I0M2Y0NWRiYTRlMDk3MzJkNDJlYWYzNGE3ZjUzYTY5MTgyMjgxZmQyNmY3ODUwYzQzODNhNTllYTZlODc1ZjUiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2U3ZGZlYTE2ZGM4M2M5N2RmMDFhMTJmYWJiZDEyMTYzNTljMGNkMGVhNDJmOTk5OWI2ZTk3YzU4NDk2M2U5ODAiCiAgICB9CiAgfQp9",
                "f1Vl75b94xDLjX6/4tfBC8D+OUJdP4x6pOpTxM41XO5ejQD3Rcu1N/zXAFMu/+fRUOdCJI9Atdc+fRaTHyTjYssXNkm3WRNLAYReJJs5aIwN6G5Y3ERnIxJdYes4a5Zugvm6mjzgUYKSky0+qSqOE/gnNFN4ekbFAZ6ralsmL6yi8OrxIg9C9tJCX3mO1xFdFx0cJvHSmYoELlzZY2FLNWqHceYiKhSKMya4dnrcG0xwRvbBHe+BJwpKkEgo07KEUe0FqYejAy+ZcqodV8d9yEx/+t4Pqy126o2zGjajdlt95kX2aXso9Jn0PFYjpJWKaRfBjKec5+nnxnAumT76hCsQSkB1bPaurpainSYdLLM0OIh5H0pyWGAK9PcLUbr/O/dpepXtDxb8DgToECqeHKbSsvSxBaC9AjSfC+ljh69sZ9kT47aiqeQj11v96s+Nc4cRlNPnxF2mjGkMUt//GMwkwacX7qeWOBnHyR1ITPemj82BKIArX/ZPBVDU92IvgxnNL8a6AXqvIWBI1JZ509KXawKyUXIby6DvmzInBrTZdRFKbzDT4NVyjsZpFAasBAKZ2R+o30aUdS4WATjFy26vvMsfLPC+27RR5ps9PlvSwK68e5D/CFxgQEgg46M50hkFMvRINXdKL2kpKQo60uGwxKou8GmBNo6dOdixoUU=",
                "3929e052bad74b7481774d5ba591fbd4");

        registerSigned("Luffy", "http://textures.minecraft.net/texture/43c9af51fe4e9a373f99a35d9b03e2e98cd9056fa6bfaa92e15c81a61ff32b98", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NTkyOSwKICAicHJvZmlsZUlkIiA6ICIxMmM0ZTE4NjU3MDA0MjM3YTZiODU2YzA1ZjcyYWFiMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJMdWZmeSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80M2M5YWY1MWZlNGU5YTM3M2Y5OWEzNWQ5YjAzZTJlOThjZDkwNTZmYTZiZmFhOTJlMTVjODFhNjFmZjMyYjk4IgogICAgfQogIH0KfQ==",
                "uBnm8xWUdCKAgVEUaNsf9/8NYIb/HuVeaPMGjQOBVpxtkX+Jw1Fge5OJSxSw6TAAINDLCiAdgBMhTVfz+TJWg+4WkDxUOPYAFT6sFgMqjXHL4I0gf7dLbTZh47O7/JID4nGVuEQgH5qmtn+ggXEpHKPMC4JobRp3/aFPVm1xy3JqF2KXz42cZehnrdIBN01p32Gj3ZlGWlJsqc6SYoZCdRKlsVKuqK0WatmvA9lBnsd1nugo3DVd4GNBQEzcwKnAjjePbWtrb2ziB2Ui+XIx5nh7ULoHem+HCHRKiSbghGfYz0Ya5N6rHqW4r3VEbcy1OeBAq1OWf4XPXyHq5+mdCfG0cuMWbsyEQ5eLB4FfG3rUajj+BsxHuWlET9cSiYMAhXgRy3xHSPbqk2x4FFBp5xqvq2QIlP4gfHNhMKZwJCC8WdWfzR7FrkLR4TByR2lJvpmHQ/bZT/qPS7ApVDRb1Ioy1qPVnOGXcOX5X+NQSs6D1dFiO0Pni1HVjPfYGH5oW8p+aUJPp8nsfIk2MrQ76MOU48lG1mftQAhzrjk7SBSt2BNP3eC6c1+etVrgK7mIDGmzLIOWDxTZkvs/GjMekGyI6vk10so3vJFMoGil1/dxW4r8KWxZfVC6aLuiCNKIbcTSsoIYeZmkda5LonWSRhPTyDi5a/yMFJ4O2fuOiJ8=",
                "12c4e18657004237a6b856c05f72aab1");

        registerSigned("Naruto", "http://textures.minecraft.net/texture/39441d14cd6f9fd3017df5025a62027b51375e364e4c496056cac416d6bbe0d5", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NjI0NSwKICAicHJvZmlsZUlkIiA6ICIwZGM0MmZhY2M1ODI0YmQ0OTE2NzllMGJjMmM5NzJjZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYXJ1dG8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzk0NDFkMTRjZDZmOWZkMzAxN2RmNTAyNWE2MjAyN2I1MTM3NWUzNjRlNGM0OTYwNTZjYWM0MTZkNmJiZTBkNSIKICAgIH0sCiAgICAiQ0FQRSIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjhkZTRhODE2ODhhZDE4YjQ5ZTczNWEyNzNlMDg2YzE4ZjFlMzk2Njk1NjEyM2NjYjU3NDAzNGMwNmY1ZDMzNiIKICAgIH0KICB9Cn0=",
                "dNj8JEjgwTPopK+m8feVWEEJmUzGLb3ASzoS8MVDDq3SPyPfxo1OZG+EBSAEe1LGRqGNhawoE7kJuIYS21M6oxUUoUeGrFhH0eiQQaio9IXX9y+HakLlNs2eQgrG74WrZb7FWNRdxOJRmwNtaEgbRYUVi8UDZqehHUuDOGbcRp1PDNqzsyZXrlBC7kETZgGWMh/oQu3ob862hCt9tdkhXUev/m1eVS1sKvHiCNBkO7oJbqX3eeKQaM7Q61885VHwv9ayEOOJJ/IRtRP3m+Glx8dDMK1vpA4lCFiImdMD8pm9VBfDY7dP1NI1MnqE8n0v+pQ/wlI1SY/lpT7MFJKOmgxf4AGYKmU/EKMQ//M1BHuznAsU42j0Y2ZLnmtbig5orah1xRMCNyXZiFQunRQGpPteGPt0oKv06+QUirGbMRE+STexjwafXs/wybsPO8J0Oc+I4bR2I+e1rz6+yqCmOKc5d613YmY7l9aqmrkDqqczYZ1sBPtX/aOZUmL4jJg6VhqBkzX0K1xlw2WkegGtxCj2DaIFHQCc74QTiYo8T0t7LG2ni4g80NlUzn34Vh+KfLiAyO3WV2NlFv3c6oeUTBtMhv0M+85AyV6iGNCN3/NbVIlOThr7c58jp7tcbMNkaL0xgIHFwE6oNLZfzaionltXvN6tPzhsz4ShIInmI9g=",
                "0dc42facc5824bd491679e0bc2c972ce");

        registerSigned("Kirito", "http://textures.minecraft.net/texture/42c772db60d96ba93cd24b66bedd73e97201b741f478c298b73fb9e28b928c20", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NjkzNiwKICAicHJvZmlsZUlkIiA6ICJhNDg5ZGVhYzlkMzE0YjliYmEzNWRiOTkzZjZlZWI3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJraXJpdG8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDJjNzcyZGI2MGQ5NmJhOTNjZDI0YjY2YmVkZDczZTk3MjAxYjc0MWY0NzhjMjk4YjczZmI5ZTI4YjkyOGMyMCIKICAgIH0KICB9Cn0=",
                "usd8sB1teRm5rrOQZrCMken5FQ7dGtS91wvFVCbm5VhVgG4Yx+K3YGiTbLfEEUuZ/bC1V+pqrOlZmRFDuHOS139bn8HIKt1X/m3ZqL8Pf8/DQNXZLjfsczrSNrv2cRzv7S548hP5/Dpg+zyML4wg6lz+0uvtKSKnvflf/RMzJrJke1YnYiQYJWb8ESc/NjjO28iNsKpebdQT3VSMV4YUfpocz+Nu8Pj+ZijZxw2d5umIwQehqeCoywQcEg9N4qu5w3lNEHBT92TXHy1B4zwEsBm8InclCxBrSG1dTkCewLhJ6NYT6LpJY/wIqj0vHAL2zMdDOH1aN/iLLrImtAYcgeLHkuiFjPCJz+11EETShSUlZeIt9Ek1Fkuhi3htmx0yLBU+VP//IDb2auiSZUhBAj3i8xcDfrkTP+wdb+ZQTpE+clZnDB7NGf+9aOWz57jvHqam8ok+ZfCWCyG9ezKCCziGMfdeKo0je66yVJMLlBdSwYDSFzgL1ODdNtY7Q/IGR0rRBvNUlNoGUMRCZJ5NTZ/KJ/hnnf+MSfvrb3PDv68j6GGGPOA++H3OXv1v8yjDZbGBI85aC2dhmPM1C3HqYlCvpvbHTh18Rmfva1VX1z/+huw2DqYNgSbrtF7JRcYNEIF+jB95+WiPp81S651rGZI9bRlByO/WVud65DHAT8o=",
                "a489deac9d314b9bba35db993f6eeb79");

        registerSigned("Zoro", "http://textures.minecraft.net/texture/5dc8add1961ebd5949c844227fb7175af6d0f207dfcee68de9fc15c716a3bbe2", true,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0Nzg3MiwKICAicHJvZmlsZUlkIiA6ICI3ZWZkYTg1OTAwMzg0NmIyOWMyN2EzN2NjMzc3NmJkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJab3JvIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVkYzhhZGQxOTYxZWJkNTk0OWM4NDQyMjdmYjcxNzVhZjZkMGYyMDdkZmNlZTY4ZGU5ZmMxNWM3MTZhM2JiZTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iMGNjMDg4NDA3MDA0NDczMjJkOTUzYTAyYjk2NWYxZDY1YTEzYTYwM2JmNjRiMTdjODAzYzIxNDQ2ZmUxNjM1IgogICAgfQogIH0KfQ==",
                "cJxDZT5ZTT2GDN1wv4ZpqVpOWg46JwboQPW5PsNFu2A5Cd3SiLY2EFH1yn1I1HQXtrIk/+95SpWfkKoXZLWIZ3qu0wNLXo0jewJJ22VJHIEu3+vBoQ5U3nKPS70UwGEzIknN96zNfn0UHy90Dd2QEjw9LPv5c3fXFOV0cF6ZpH5pRYsNV1tW2nVXc/HrS8S3kCHGVfB0VRU0Y2AnFBPBhh6b4upHC22iIRXSEKVPTwQiYhlkhKetGisW1gTWAx6/qwys9Gr+ZWD4H5yvLnW3d1t4vUpIa46UF5ThKnP2tJlnYHBpV6AkwYgbkKa6OpAFlczBTO9BQvuYYG3hirVmxKxJHrU+y1lZNfV/geKo7qgW+FG0Cs6oCOUaTqloRi5yyRlh9va7b1ih81g9Npi7uguLu5Yjuo8OkbhiReFzpDwSAv+2Br2eT85rDh4XDxJeOhPbnJvotWblIjDAmxSNOlf0P9m2bsObA1/KmoiMPRB26TLwr66hXFqQt+ydrsTeVSNjM7EolH3VJE1iD0xuKmICNG0dryb9j+L3+guK/31V94LW2DY0Q2Au2DkW3ODYZCl4QrE7Z89VF8ANXgZdStVJGmZUDVnUcTy+9PscMME83PgBr3TxnGOFcUx+Ic6sT2C4wamnolsKcsLJRu76ze3kDVda+V422/RzkFc0krU=",
                "7efda859003846b29c27a37cc3776bd4");

        registerSigned("Gojo", "http://textures.minecraft.net/texture/41754d71350fb3624c3fd3d8ee68c29efc11cb9eccc996be07422154602c2e4c", false,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0NzU3NiwKICAicHJvZmlsZUlkIiA6ICI4YjZlNjIzNmY2NTE0M2RiYmY3NDg1ZjZlZjQ1OTk3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHb2pvIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQxNzU0ZDcxMzUwZmIzNjI0YzNmZDNkOGVlNjhjMjllZmMxMWNiOWVjY2M5OTZiZTA3NDIyMTU0NjAyYzJlNGMiCiAgICB9CiAgfQp9",
                "ox5z8qBEvSdmzf/r2QUnslUPe9Blu3kHqK0tVI6aS2y8EiHw+q84XKHuBPjSYGJVLCfoMQVZCtDpjkm0S3QNx3iZXvAcN34ZLFiDR5dFt/HOB4hF/QA84jRWSuNAJ4HUwsMHd9nwQydUgWegKwg4JsiAwxDOnX9sQ3uTRhkhdxm1sEOU/xFvJHW5AIwz4/zBINvt+Doh+mGP1mJ12LLYf7KvusRRleN3fu84dpBYPq+VK1ebHeOkfOci8YP0VgrVxPrEIbi8hMBRT9Ovv8iYx9u6pb0tz8APp8l3oBNJa8g9aIKOuiCds/NQJz1JvoBC5eQIYLu8363AQ5zsjAQyT0/sgi84PRAGRMEyq2GtdDwduPlOlvZcLLyXZVQKHLqgRECfkq5VUs9iTBmOShS1TsGvSBMRpQR/uzI9MSenOKhkQizB3rHIJmr4oAhxXnD9sWP9qcuc7k7+NiSvO0464fG3RQNYRp9BpF52eRxVnVZVBLT/j/1/UphgVfHUSsfvJlXApCPi2IbGqq+WNqFAqKOYiBRrSSa5nGkFWs+wLlbyEnJA3RXUfiQ6p9VwbR+t/9yhmrshFMjuofkjS/P2jvZT+In8+iC5dvoZZfDnmTgCBJbVxwARmLpiHEWDMkekmDOiwY0S3oRZ6/7LE3DlujKYz7CgbxiRsIYeP3hEgbM=",
                "8b6e6236f65143dbbf7485f6ef459979");

        registerSigned("Tanjiro", "http://textures.minecraft.net/texture/2c7f5c019d62f1515b0ecc53a63acbc725199bdc4de93338a224249ffc0fc224", true,
                "ewogICJ0aW1lc3RhbXAiIDogMTc4OTg1NTA0ODE4NywKICAicHJvZmlsZUlkIiA6ICI0Y2FjZjcyZGE4Nzg0YTRlYTEyNDcyYTdlNjBmM2RlYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUYW5qaXJvIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJjN2Y1YzAxOWQ2MmYxNTE1YjBlY2M1M2E2M2FjYmM3MjUxOTliZGM0ZGU5MzMzOGEyMjQyNDlmZmMwZmMyMjQiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                "dxVw4jBvM9caiibrtpRYlxsGqZalxHyAueKj1V5sZVaje4ZfA6udUKywWErPj1jw7Yun/iBNOGY3h37kNCjvaBp0cF5C8bMPVKxmcfN5+U0da043SCTFhjG2CHUYfSBdzbaLgymewATD5+CLbLuZvn0yPBxmFnztiIS7nwYELhh/6DXp5Rfg/+3X8y/VOb5pYWCm4e/IOddWs6eCJifDvV1l2E/wiubj5YgG5YfPPfs8nYeKNEr1an3BGC9eqXpAL2PwOlMI1XVHDbAHvjKdN7xGvkpHTTurRaWp/xYP8s5RdVEHuFGvzhtJkFF9DQVbpbpDR4lKTL42KipnrKUcM9B0z1phcdQCkUreiDZuMsSM+xYHPljL/Ts5hw/sKSE89vISSgQomo+YerDgWkAgSS2VBoZEic2FI9mJI2XSTZ70YHvAymj01clfk2LSUHWn4UU1k4uAQPit0jyT9B4SNb5zUS7qzRxNDalDWTACh63L5B4z/eHeSDU2J2nTksQcOzwT4RIyHAOnyroxP0EQLswrb4s8lTQoluKB8l/KqHzcggBbsfgxhkeWIHDbvBeeab2WWd/9XyqcdtnXqzvUkiP9r3iXR60jobaOEPd6KnWnfi0QayTfsxq69YoOI2y71jSP2/L6PJK5ybyokoqRCwe2KCdE9Jr7u5MeJBtBzFY=",
                "4cacf72da8784a4ea12472a7e60f3dea");
    }

    private static void registerSigned(String name, String textureUrl, boolean slim, String base64Value, String signature, String profileId) {
        PRESETS.put(name.toLowerCase(Locale.ROOT), new PresetSkin(name, textureUrl, slim, base64Value, signature, profileId));
    }

    public static PresetSkin getPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return PRESETS.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean hasPreset(String name) {
        if (name == null) return false;
        return PRESETS.containsKey(name.trim().toLowerCase(Locale.ROOT));
    }

    public static Set<String> getAvailablePresetNames() {
        return Collections.unmodifiableSet(PRESETS.keySet());
    }

    public static String buildCustomTextureBase64(String textureUrl, boolean isSlim) {
        if (textureUrl == null || textureUrl.trim().isEmpty()) {
            return null;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl.trim() + "\"" + (isSlim ? ",\"metadata\":{\"model\":\"slim\"}" : "") + "}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
