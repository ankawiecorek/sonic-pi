;;--
;; This file is part of Sonic Pi: http://sonic-pi.net
;; Full project source: https://github.com/samaaron/sonic-pi
;; License: https://github.com/samaaron/sonic-pi/blob/master/LICENSE.md
;;
;; Copyright 2013, 2014, 2015 by Sam Aaron (http://sam.aaron.name).
;; All rights reserved.
;;
;; Permission is granted for use, copying, modification, and
;; distribution of modified versions of this work as long as this
;; notice is included.
;;++


(ns sonic-pi.synths.traditional
  (:use [overtone.live])
  (:require [sonic-pi.synths.core :as core]))

(without-namespace-in-synthdef
 (defsynth sonic-pi-piano [note 52
                           note_slide 0
                           note_slide_shape 5
                           note_slide_curve 0
                           amp 1
                           amp_slide 0
                           amp_slide_shape 5
                           amp_slide_curve 0
                           pan 0
                           pan_slide 0
                           pan_slide_shape 5
                           pan_slide_curve 0
                           gate 1
                           vel 0.2
                           decay 0.2
                           release 0.2
                           hard 0.5
                           velhard 0.4
                           muffle 0.8
                           velmuff 0.8
                           velcurve 0.8
                           stereo_width 0.2
                           cutoff 0
                           cutoff_slide 0
                           cutoff_slide_shape 5
                           cutoff_slide_curve 0
                           res 0.2
                           res_slide 0
                           res_slide_shape 5
                           res_slide_curve 0

                           out_bus 0]
   (let [note          (varlag note note_slide note_slide_curve note_slide_shape)
         amp           (varlag amp amp_slide amp_slide_curve amp_slide_shape)
         pan           (varlag pan pan_slide pan_slide_curve pan_slide_shape)
         cutoff        (varlag cutoff cutoff_slide cutoff_slide_curve cutoff_slide_shape)
         cutoff-freq   (midicps cutoff)
         use-filter    (> cutoff 0)
         res           (lin-lin res 1 0 0 1)
         res           (varlag res res_slide res_slide_curve res_slide_shape)
         freq          (midicps note)
         vel           (clip vel 0 1)
         vel           (lin-lin vel 0 1 0 4)
         vel           (* vel 127)
         hard          (clip hard 0 1)
         hard          (lin-lin hard 0 1 -3 3)


         snd           (mda-piano {:freq freq
                                   :gate 1
                                   :vel vel
                                   :decay decay
                                   :release release
                                   :hard hard
                                   :velhard 0.8
                                   :muffle 0.8
                                   :velmuff 0.8
                                   :velcurve velcurve
                                   :stereo stereo_width
                                   :tune 0.5
                                   :random 0
                                   :stretch 0
                                   :sustain 0.1 })

         [snd-l snd-r] snd
         snd-l         (select use-filter [snd-l (rlpf snd-l cutoff-freq res)])
         snd-r         (select use-filter [snd-r (rlpf snd-r cutoff-freq res)])
         [new-l new-r] (balance2 snd-l snd-r pan amp)]
     (out out_bus [new-l new-r])
     (detect-silence snd 0.005 :action FREE) )

   )


   (defsynth sonic-pi-synth_violin
    "synth violin taken from Roger Allen's gist
    https://gist.githubusercontent.com/rogerallen/5992549/raw/2e4ed49bef990817e83981d812ab609e1b3bb901/violin.clj
    inspired by Sound On Sound April-July 2003 articles."
    [note 52
     note_slide 0
     note_slide_shape 5
     note_slide_curve 0
     amp 1
     amp_slide 0
     amp_slide_shape 5
     amp_slide_curve 0
     pan 0
     pan_slide 0
     pan_slide_shape 5
     pan_slide_curve 0
     attack 1.5
     decay 1.5
     sustain 0.8
     release 1.5
     attack_level 1
     sustain_level 1
     env_curve 2
     cutoff 107 ;; ~ 4000 Hz
     cutoff_slide 0
     cutoff_slide_shape 5
     cutoff_slide_curve 0
     vibrato_rate 6
     vibrato_depth 0.02
     vibrato_delay 0.5
     vibrato_onset 0.1
     out_bus 0]
    (let [note       (varlag note note_slide note_slide_curve note_slide_shape)
          amp         (varlag amp amp_slide amp_slide_curve amp_slide_shape)
          amp-fudge   1.1
          pan         (varlag pan pan_slide pan_slide_curve pan_slide_shape)
          cutoff      (varlag cutoff cutoff_slide cutoff_slide_curve cutoff_slide_shape)
          freq        (midicps note)
          cutoff-freq (midicps cutoff)

          ;; 3b) portamento to change frequency slowly
          freqp       (slew:kr freq 100.0 100.0)
          ;; 3a) vibrato to make it seem "real"

          ;; NOTE: this was the original vibrato implementation from Roger's code
          ;; freqv  (vibrato :freq freq :rate vibrato_rate :depth vibrato_depth :delay vibrato_delay :onset vibrato_onset)
          ;; freq   freqv
          ;; but this didn't seem to work on the ancient version of SuperCollider we use on the RPi
          freqv       (*
                       ;; delay before vibrato gets to full strength
                       (env-gen:kr (envelope [0 0 vibrato_depth] [vibrato_delay vibrato_onset]))
                       ;; actual frequency to add to the original pitch
                       (sin-osc:kr :freq vibrato_rate))

          ;; Calculate the vibrato in midi note (log frequency) then convert back
          freq        (midicps (+ (cpsmidi freq) freqv))
          ;; the main osc for the violin
          saw         (saw freqv)
          ;; a low-pass filter prior to our filter bank
          saw1        (lpf saw cutoff-freq)
          ;; the "formant" filters
          band1       (bpf saw1 300 (/ 3.5))
          band2       (bpf saw1 700 (/ 3.5))
          band3       (bpf saw1 3000 (/ 2))
          saw2        (+ band1 band2 band3)
          ;; a high-pass filter on the way out
          saw3        (hpf saw2 30)
          snd         (* amp-fudge saw3)
          env         (env-gen:kr (env-adsr-ng attack decay sustain release attack_level sustain_level env_curve) :action FREE)
          ]
      (out out_bus (pan2 (* amp-fudge snd env) pan amp)))))

(comment
  (core/save-synthdef sonic-pi-piano)
  (core/save-synthdef sonic-pi-synth_violin))
