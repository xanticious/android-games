# Drift Racer — Design Document

## Overview
A high-definition top-down racing game inspired by RC Pro-Am. The player drives a single car across multiple handcrafted courses, chasing personal best lap times. No opponents appear on track — this is pure single-player time trial in the spirit of Trackmania. Mastery of drifting is the core skill.

## Visual Style
- Top-down 3/4 perspective (slight isometric tilt for depth).
- Tracks feature richly detailed surfaces: asphalt, gravel, grass, dirt, and painted markings.
- Car: highly detailed RC-car-style vehicle with a subtle shadow below it.
- Drift: visible tire marks drawn in real time on the track surface (fade out after 10 seconds).
- Speed lines appear at the screen edges during top speed.
- Track boundaries: colored curbs, barriers, and grass. Driving over grass slows the car.
- Weather layer option per course: dry, light rain (wet track, lower grip), night mode (headlights on).
- Camera follows the car with a slight lag for a sense of momentum.

## Screen Layout
```
┌─────────────────────────────────┐
│  [Lap: 1/3] [Time: 00:00.000]  │  ← HUD (top)
│  [Best: 00:00.000]              │
├─────────────────────────────────┤
│                                 │
│     TRACK (top-down view)       │
│         [CAR]                   │
│                                 │
├─────────────────────────────────┤
│  [Steering Wheel]  [Gas/Brake]  │  ← Controls (bottom overlay)
└─────────────────────────────────┘
```

## Controls
- **Steering**: left thumb virtual joystick (see `design/common/virtual-joystick.md`). Horizontal axis controls steering angle; vertical axis is unused.
- **Throttle**: right thumb hold area — press and hold to accelerate, release to coast, swipe down to brake.
- Alternatively, a dedicated Brake button alongside the throttle zone.
- Drift is physics-driven: entering a corner at speed with steering applied causes the rear to slide; the player must counter-steer to control the drift angle.

## Gameplay Loop

### Session Structure
1. Player selects a course from the course list.
2. A 3-second countdown begins.
3. Player completes a set number of laps (default: 3).
4. After finishing, the result screen shows:
   - Each lap time.
   - Best lap time for this session.
   - Personal best for the course (all-time).
   - Total race time.
5. If a new personal best was set, a gold record indicator is shown.
6. Player can retry immediately or return to course select.

### Courses
- Multiple handcrafted courses, each with a distinct theme:
  - City Circuit (tight urban corners)
  - Mountain Pass (long sweeping curves with elevation changes)
  - Industrial Park (mixed surface, technical layout)
  - Beach Run (wet sand, lower grip)
  - Forest Loop (narrow tree-lined track, multiple chicanes)
- Each course has a "par time" (a target time that earns a gold medal).
- Courses unlock progressively as the player beats par times.

### Car Physics
- Single car model — no upgrades, no unlocks.
- The car has realistic (but accessible) drift physics:
  - Grip phase: steering input proportionally rotates the car.
  - Drift phase: triggered by entering a corner above a speed threshold.
  - In drift, the car's rear slides outward; steering becomes counter-intuitive (counter-steer to stabilize).
  - Throttle during drift maintains the slide; braking stops it.
- Hitting track boundaries: slight bounce, speed reduction of ~30%.
- Grass/dirt: speed reduction of ~50%.

### Local Statistics (stored on device)
- Per course: best lap time, best 3-lap total time, medal earned (bronze/silver/gold).
- Total distance driven across all courses.
- Total number of laps completed.

## Difficulty
No in-game difficulty setting — the challenge comes entirely from the course design and the player's driving skill. Par times are calibrated to be achievable with practice.

## State Machine
```
Idle
 └─ CourseSelected → Countdown
Countdown
 └─ CountdownComplete → Racing
Racing
 ├─ LapCompleted → Racing (next lap, record lap time)
 └─ FinalLapCompleted → RaceFinished
RaceFinished
 └─ (show results, player choice) → Idle or Countdown (retry)
```

## HUD
- Current lap / total laps: top-left.
- Current lap timer (stopwatch, live): top-center.
- Best lap time for this session: top-right.
- Speed indicator: bottom-center, subtle arc gauge.
- Mini-map: bottom-left corner, shows track outline and car position dot.
- Course name: shown briefly at race start, fades after 3 seconds.
