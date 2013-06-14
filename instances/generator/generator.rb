#!/usr/bin/env ruby
# encoding: UTF-8

require 'json'

instance = Hash.new

# read in parameters
raise "Must specify number of events, rooms and timeslots" unless ARGV.size >= 3
n_events = ARGV[0].to_i
n_rooms = ARGV[1].to_i
n_timeslots = ARGV[2].to_i
raise "Need at least 4 timeslots" unless n_timeslots >= 4

# Create timeslots
instance[:timeslots] = []
n_timeslots.times do |i|
  timeslot = Hash.new
  timeslot[:id] = "t#{i}"
  instance[:timeslots] << timeslot
end

# Group timeslots
doublets = []
triplets = []
n_timeslots.times do |i|
  if i%2 == 0
    if i+1 < n_timeslots
      doublet = [instance[:timeslots][i], instance[:timeslots][i+1]]
      doublets << doublet
    end
    if i+2 < n_timeslots
      triplet = [instance[:timeslots][i], instance[:timeslots][i+1], instance[:timeslots][i+2]]
      triplets << triplet
    end
  end
end

#Create rooms
instance[:rooms] = []
n_rooms.times do |i|
  room = Hash.new
  room[:id] = "r#{i}"
  instance[:rooms] << room
end

#Create events
instance[:events] = []
n_events.times do |i|
  event = Hash.new
  event[:id] = "e#{i}"

  # assign possible timeslots
  event[:possibleTimeslots] = []

  # choose event duration
  r = rand()
  if r < 0.4
    # duration = 1

    choose_n = (instance[:timeslots].size.to_f/2).ceil
    instance[:timeslots].sample(choose_n).each do |t|
      possibleTimeslots = Hash.new
      possibleTimeslots[:weight] = rand(3) + 1
      possibleTimeslots[:timeslots] = [t[:id]]

      event[:possibleTimeslots] << possibleTimeslots
    end
      
  elsif r < 0.9
    # duration = 2

    choose_n = (doublets.size.to_f/2).ceil
    doublets.sample(choose_n).each do |ts|
      possibleTimeslots = Hash.new
      possibleTimeslots[:weight] = rand(3) + 1
      possibleTimeslots[:timeslots] = []
      ts.each do |t|
        possibleTimeslots[:timeslots] << t[:id]
      end

      event[:possibleTimeslots] << possibleTimeslots
    end
   
  else
    # duration = 3

    choose_n = (triplets.size.to_f/2).ceil
    triplets.sample(choose_n).each do |ts|
      possibleTimeslots = Hash.new
      possibleTimeslots[:weight] = rand(3) + 1
      possibleTimeslots[:timeslots] = []
      ts.each do |t|
        possibleTimeslots[:timeslots] << t[:id]
      end

      event[:possibleTimeslots] << possibleTimeslots
    end
  end

  # assign possible rooms
  event[:possibleRooms] = []
  n_choose = (n_rooms.to_f / 5).ceil
  instance[:rooms].sample(n_choose).each do |r|
    possible_rooms = Hash.new
    possible_rooms[:weight] = rand(3)+1
    possible_rooms[:rooms] = [r[:id]]
    if (rand() < 0.1)
      r2 = instance[:rooms].sample
      if r2 != r
        possible_rooms[:rooms] << r2[:id]
      end
    end
    event[:possibleRooms] << possible_rooms
  end

  instance[:events] << event
end

#Create conflicts
min_conflict_size = 2
max_conflict_size = (n_timeslots / 2).floor
avg_conflict_size = (min_conflict_size.to_f + max_conflict_size.to_f) / 2
n_conflicts = (Math.log(0.05)/Math.log(1-avg_conflict_size/n_events.to_f)).ceil # an event occurs in a conflict with at least 5 % probability

instance[:restrictions] = []
n_conflicts.times do |i|
  conflict_size = rand(max_conflict_size - min_conflict_size + 1) + min_conflict_size
  conflict = Hash.new
  conflict[:id] = "c#{i}"
  conflict[:type] = "time-conflict"
  conflict[:events] = []
  instance[:events].sample(conflict_size).each do |event|
    conflict[:events] << event[:id]
  end

  instance[:restrictions] << conflict
end

puts JSON.generate(instance)
