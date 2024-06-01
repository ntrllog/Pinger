# Pinger

An Android app of a custom stopwatch

App available on [Google Play Store](https://play.google.com/store/apps/details?id=ntrllog.github.io.pinger)

When working out in the gym, I like to do some exercises for a certain amount of time rather than for a number of reps. However, a simple stopwatch doesn't work for me because from the time I press start to the time I actually start my set, some seconds have passed. To solve this, this app has a start period so that I can get into position (e.g., picking up dumbbells) during the start period.

For my ab workout, I do a number of different exercises for 1 minute (each exercise) with 8-10 seconds rest in between each one. Continuous mode is there so I don't have to keep starting the app when the time is done.

Continuous mode also helps me keep track of my rest period so I don't rest too long.

It really helps to have an auditory cue tell me when the specified time has passed. That way, I don't have to keep looking at my phone. In fact, if I don't know how much time I have left, then I won't cheat my reps by giving up early.

## Screenshots
![Alt text](/screenshots/non_continuous.jpg?raw=true)
![Alt text](/screenshots/continuous.jpg?raw=true)
![Alt text](/screenshots/help.jpg?raw=true)
![Alt text](/screenshots/running.jpg?raw=true)

## Resources

Sounds downloaded from https://www.pacdv.com/sounds/index.html

## Known Bugs

- Pausing, stopping, starting too fast (e.g., pausing right when the ping period starts or stopping and starting immediately after) can lead to unintended consequences.
  - This is because I keep track of how many seconds have passed, instead of keeping track of milliseconds or nanoseconds.