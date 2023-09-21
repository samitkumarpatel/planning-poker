package com.example.planningpoker.models;

import java.util.List;
import java.util.UUID;

public record Room(UUID id, List<String> cards, List<Member> members) {}
