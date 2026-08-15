Document 1: Master Architecture

Project Name: Project Coach (working title)

Universal Real-Time Game Coaching Framework

Table of Contents
1. Executive Summary
Project vision
Problem statement
Goals
Non-goals
MVP scope
Long-term vision
Success metrics
2. Core Philosophy
Coach, don't automate
Human remains in control
Accessibility first
Modular encounter definitions
Deterministic behavior
Extensible architecture
Multi-game potential
3. High-Level Architecture
Overall System Diagram
Component Overview
RuneLite Plugin
Coaching Engine
Encounter Engine
Trigger Engine
Overlay System
Audio Engine
Knowledge Compiler
AI Knowledge Generator
Configuration Manager
Logging System
4. Application Architecture
Frontend
Overlay rendering
Settings UI
Boss selection
Debug panel
Backend (optional future)
Encounter repository
Update service
Community sharing
Analytics (optional)
Local Components
JSON parser
Audio assets
Cache
Configuration
Event processing
5. RuneLite Integration
Plugin lifecycle
Event subscriptions
NPC tracking
Animation listeners
Projectile listeners
Graphics listeners
Tick events
HP monitoring
Prayer state
Inventory state
Player movement
Tile tracking
6. Coaching Engine
Responsibilities

Decision making

Priority resolution

Callout scheduling

Cooldown handling

Queue management

State management

Prediction

7. Encounter Engine

Encounter definition format

Boss metadata

Phase definitions

Mechanics

Conditions

Transitions

Recovery logic

Failure states

Completion states

8. Trigger System

Animation triggers

Projectile triggers

Graphic object triggers

NPC spawn triggers

HP triggers

Tick timers

Player state triggers

Location triggers

Custom rule triggers

Composite triggers

9. Knowledge System

Boss definitions

JSON schema

Validation

Versioning

Import/export

Community packs

Future AI generation

10. AI Knowledge Compiler

Input sources

Wiki parsing

Manual notes

Prompt engineering

Schema conversion

Validation

Human review

Publishing pipeline

11. Audio System

Callout queue

Priority system

Interruptions

Voice packs

Localization

Volume control

Cooldowns

Timing offsets

12. Overlay System

Prayer indicators

Countdowns

Boss timeline

Next mechanic

Safe tiles

Status indicators

Boss HP

Mini HUD

Debug overlays

13. Configuration System

Global settings

Per-boss settings

Audio settings

Visual settings

Accessibility

Import/export

Profiles

14. Data Models

Boss

Encounter

Mechanic

Trigger

Callout

Overlay

Timeline

Configuration

Player state

Boss state

15. JSON Schema Specification

Complete schema

Examples

Validation rules

Version compatibility

Migration strategy

16. File Structure

Complete project tree

Plugin layout

Resources

Knowledge packs

Tests

Documentation

Assets

17. Error Handling

Graceful failures

Missing definitions

Invalid triggers

Fallback behaviors

Logging

Recovery

18. Performance

Tick processing

Memory usage

Caching

Audio latency

Rendering optimization

Profiling

19. Security

Plugin safety

Local-only operation

No automation guarantees

Input validation

Data integrity

20. Testing Strategy

Unit tests

Integration tests

Boss simulations

Replay testing

Regression testing

Performance benchmarks

21. Future Expansion

Multiple games

Community repository

AI-generated encounters

Replay analysis

Voice coaching improvements

Adaptive coaching

Cloud sync (optional)

Machine learning research

Document 2: Sprint Roadmap
Phase 1 — Foundation

Sprint 1

Project setup
RuneLite plugin skeleton
Build pipeline
Settings page
Logging

Sprint 2

Event system
Tick listener
Animation listener
Projectile listener
Graphics listener

Sprint 3

Audio engine
Overlay engine
Notification framework
Configuration system
Phase 2 — Core Coaching

Sprint 4

Encounter engine

Sprint 5

Trigger engine

Sprint 6

Rule processor

Sprint 7

State management

Sprint 8

Timeline prediction

Sprint 9

Priority scheduler
Phase 3 — Boss Support

Sprint 10

JSON schema

Sprint 11

Boss loader

Sprint 12

Nex implementation

Sprint 13

Inferno implementation

Sprint 14

Theatre of Blood

Sprint 15

Tombs of Amascut

Sprint 16

Chambers

Sprint 17

Colosseum
Phase 4 — User Experience

Sprint 18

Settings overhaul

Sprint 19

Overlay improvements

Sprint 20

Audio improvements

Sprint 21

Accessibility

Sprint 22

Debug tools

Sprint 23

Profile management
Phase 5 — AI Knowledge Pipeline

Sprint 24

Wiki parser

Sprint 25

AI schema generation

Sprint 26

Validation tools

Sprint 27

Knowledge editor

Sprint 28

Knowledge testing

Sprint 29

Packaging system
Phase 6 — Polish

Sprint 30

Optimization

Sprint 31

Testing

Sprint 32

Documentation

Sprint 33

Release preparation

Sprint 34

Community beta

Sprint 35

Version 1.0
Document 3: Implementation Guide
Part I — Development Standards
Coding conventions
Naming conventions
Project organization
Dependency management
Documentation standards
Testing requirements
Part II — Environment Setup

RuneLite development

JDK

Gradle

IDE setup

Debugging

Hot reload

Part III — Core Systems

Implement Plugin Lifecycle

Implement Event Manager

Implement Trigger Engine

Implement Encounter Engine

Implement Coaching Engine

Implement Overlay Manager

Implement Audio Manager

Implement Configuration Manager

Part IV — Data Layer

Design JSON schema

Implement parser

Validation

Caching

Version handling

Serialization

Part V — Boss Development Workflow

Creating a new boss

Building encounter phases

Adding triggers

Adding callouts

Overlay configuration

Testing

Validation checklist

Packaging

Part VI — AI Knowledge Pipeline

Extract wiki information

Normalize mechanics

Generate encounter schema

Validate

Human review

Publish knowledge pack

Regression testing

Part VII — UI Implementation

Settings panels

Boss browser

Overlay widgets

Audio controls

Debug interface

Accessibility features

Part VIII — Testing

Unit testing

Replay simulations

Tick verification

Boss-specific test suites

Performance testing

Regression testing

Stress testing

Part IX — Release Process

Versioning

Build pipeline

Artifact generation

Plugin packaging

Release checklist

Documentation updates

Part X — Future Modules
Replay Analyzer
Encounter Recorder
Community Knowledge Repository
AI Voice Coach
Visual Timeline Editor
Encounter Definition Designer
Multi-game Adapter Layer
Cloud Synchronization (optional)
Telemetry & Diagnostics (opt-in)
Plugin Marketplace Integration

I also see a natural Phase 2 for this project that goes beyond coaching: a Replay & Practice Mode. By recording fight events and replaying them through the same coaching engine, users could review exactly where they missed prayers, delayed movement, or used supplies inefficiently. That would turn the project from an in-fight assistant into a full training platform, and the architecture above leaves room for that without requiring major redesign.