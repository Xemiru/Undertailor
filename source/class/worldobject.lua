--[[
	
	The MIT License (MIT)

	Copyright (c) 2016 Tellerva, Marc Lawrence

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
	
]]--


---
-- `worldobject`s are the primary functional objects within an
-- `overworldcontroller`, as well as pretty much the entire engine.
--
-- This class also holds the functions of the following classes:
--
-- * `layerable`
-- * `positionable`
-- * `eventlistener`
-- * `identifiable`
-- * `destructible`
-- * `collider`
-- * `renderable`
-- * `processable`
--
-- @classmod worldobject
-- @alias wobj
---

local wobj = {}

---
-- Returns the current `worldroom` this `worldobject` resives in.
--
-- @return this worldobject's current worldroom
--
function wobj:getRoom() end

---
-- Returns the current `renderable` actor used to represent this
-- `worldobject`.
--
-- @return a renderable used to represent this worldobject
--
function wobj:getActor() end

---
-- Sets the `renderable` actor used to represent this `worldobject`.
--
-- @tparam renderable renderable the new actor to set
--
function wobj:setActor(renderable) end

---
-- Returns whether or not this `worldobject` is actually being
-- rendered.
--
-- @return if this worldobject is visible
--
function wobj:isVisible() end

---
-- Sets whether or not this `worldobject` is actually being
-- rendered.
--
-- @bool visible if this worldobject will be visible
--
function wobj:setVisible(visible) end

---
-- Returns whether or not this `worldobject` will be kept when the
-- Overworld controller changes rooms, if it was inside the active
-- room.
--
-- This is automatically true if this worldobject is the Overworld's
-- character object.
--
-- @return if this worldobject is persistent
--
function wobj:isPersistent() end

---
-- Sets whether or not this `worldobject` will be kept when the
-- Overworld controller changes rooms, if it was inside the active
-- room.
--
-- @bool visible if this worldobject will be persistent
--
function wobj:setPersistent(visible) end

---
-- Bounding shape functions.
-- @section

---
-- Applies a new bounding polygon to this `worldobject`.
--
-- The varargs to be passed are to contain the separate points
-- making up the bounding shape. Polygons are automatically closed,
-- you cannot make open polygons using this function. The passed
-- varargs must contain an equal amount of arguments, and must
-- resolve to at least 3 points. At point 0, 0 is the origin location
-- of this `worldobject`. Points are relative to the origin by pixel
-- units.
--
-- @usage -- With the points (-3, -3), (0, 3), (3, -3) to form a
-- -- triangle, the parameters passed to this function would be:
-- obj:addBoundingPolygon(-3, -3, 0, 3, 3, -3)
--
-- @tparam vargs ... the points creating the polygon
--
function wobj:addBoundingPolygon(...) end

---
-- Applies a new bounding circle shape to this `worldobject`.
--
-- @number radius the radius of the circle around the origin point
-- @number[opt=0.0] offsetX the horizontal offset applied to the
--   bounding circle position, with 0 being the original point at the
--   object's origin
-- @number[opt=0.0] offsetY the vertical offset applied to the
--   bounding circle position, with 0 being the original point at the
--   object's origin
--
function wobj:addBoundingCircle(radius, offsetX, offsetY) end

---
-- Applies a new bounding box shape to this `worldobject`.
--
-- @number width the width of the bounding box
-- @number height the height of the bounding box
-- @number[opt=0.0] offsetX the horizontal offset applied to the
--   bounding box position, with 0 being the original point at the
--   object's origin
-- @number[opt=0.0] offsetY the vertical offset applied to the
--   bounding box position, with 0 being the original point at the
--   object's origin
--
function wobj:addBoundingBox(width, height, offsetX, offsetY) end

---
-- Applies a new bounding chain shape to this `worldobject`.
--
-- Note that chain shapes react differently to collision. While
-- unlike polygons, they can have an infinite amount of vertices
-- they do not have any notion of mass, and thus cannot collide
-- with another chain shape. Other shape types will still collide
-- with them.
--
-- See `wobj:addBoundingPolygon` for parameter instructions.
--
-- @tparam vargs ... the points creating the chain
--
function wobj:addBoundingChain(...) end

---
-- Applies a new bounding chain shape to this `worldobject`.
--
-- This function differs in that it cleanly and automatically closes
-- the shape generated by the provided vertices.
--
-- See `wobj:addBoundingChain` for info about chain shapes.
--
-- See `wobj:addBoundingPolygon` for parameter instructions.
--
-- @tparam vargs ... the points creating the chain
--
function wobj:addBoundingChainLoop(...) end
