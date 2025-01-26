package com.deathplus;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class AiTaunts {
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY"); // Replace with your OpenAI API key
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final List<Map<String, Object>> messages = new ArrayList<>();
    private static final Map<UUID, List<Instant>> playerTauntTimestamps = new ConcurrentHashMap<>(); // Tracks player taunt timestamps


    public static void taunt(MinecraftServer server, ServerPlayerEntity player, DamageSource death) {
        UUID playerId = player.getUuid();
        Instant now = Instant.now();

        try {
            if (ConfigLoader.aiRateLimit != null) {
                // Initialize or update player's taunt timestamps
                playerTauntTimestamps.putIfAbsent(playerId, new ArrayList<>());
                List<Instant> timestamps = playerTauntTimestamps.get(playerId);

                // Remove timestamps older than 1 minute
                timestamps.removeIf(timestamp -> timestamp.isBefore(now.minusSeconds(60)));

                // Check if player is on cooldown
                if (timestamps.size() >= ConfigLoader.aiRateLimit) {
                    Instant lastTauntTime = timestamps.getLast();
                    if (lastTauntTime.isAfter(now.minusSeconds(ConfigLoader.aiCooldownMinutes * 60L))) {
                        DeathPlus.LOGGER.info("Player {} is on taunt cooldown.", player.getName().getString());
                        return;
                    } else {
                        // Clear timestamps after cooldown expires
                        timestamps.clear();
                    }
                }

                // Add current timestamp to the list
                timestamps.add(now);
            }

            if (OPENAI_API_KEY == null || OPENAI_API_KEY.isEmpty()) {
                DeathPlus.LOGGER.error("OpenAI API key not found in environment variables.");
                return;
            }

            // Prepare the request body
            Gson gson = new Gson();
            String deathMessage = death.getDeathMessage(player) != null ? death.getDeathMessage(player).getString() : "Unknown cause of death";
            Map<String, Object> requestBody = getRequestBody(player, deathMessage);
            String jsonRequestBody = gson.toJson(requestBody);

            // Create an HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody, StandardCharsets.UTF_8))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse the response JSON
            Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> openaiMessage = (Map<String, Object>) choices.get(0).get("message");
                if (openaiMessage != null) {
                    messages.add(openaiMessage);
                    String tauntMessage = openaiMessage.get("content").toString().trim();

                    // Broadcast the taunt message to all players
                    Text message = Text.literal(tauntMessage).formatted(Formatting.RED);
                    server.getPlayerManager().broadcast(message, false);
                }
            }
        } catch (Exception e) {
            DeathPlus.LOGGER.error("Error while generating taunt: {}", e.getMessage());
        }
    }

    private static Map<String, Object> getRequestBody(ServerPlayerEntity player, String deathMessage) {
        String prompt = String.format(
                "Generate a funny and creative taunt for a Minecraft player named '%s' who just died. Don't drag one joke on forever, have original ideas. Their death message was '%s'. Only say the taunt, no quotes and also no emojis.",
                player.getName().getString(), deathMessage
        );

        messages.add(Map.of("role", "user", "content", prompt));

        return Map.of(
                "model", ConfigLoader.aiTauntModel,
                "messages", messages,
                "temperature", 1.1
        );
    }
}
