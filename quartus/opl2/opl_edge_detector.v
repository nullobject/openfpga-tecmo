/*
 * Copyright (c) 2014 Greg Taylor <gtaylor@sonic.net>
 *
 * This file is part of OPL3 FPGA.
 *
 * OPL3 FPGA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OPL3 FPGA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OPL3 FPGA.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original Java Code:
 * Copyright (c) 2008 Robson Cozendey <robson@cozendey.com>
 *
 * Original C++ Code:
 * Copyright (c) 2012  Steffen Ohrendorf <steffen.ohrendorf@gmx.de>
 *
 * Some code based on forum posts in:
 * http://forums.submarine.org.uk/phpBB/viewforum.php?f=9,
 * Copyright (c) 2010-2013 by carbon14 and opl3
 *
 * Converted to Verilog and reduced to the OPL2 subset:
 * Copyright (c) 2018 Magnus Karlsson <magnus@saanlima.com>
 *
 * Fixed and refactored:
 * Copyright (c) 2020 Josh Bassett
 */

`timescale 1ns / 1ps

module opl_edge_detector #(
  parameter EDGE_LEVEL = 1,         // 1 = positive edge, 0 = negative edge
  parameter CLK_DLY = 0,            // 0 = no clock delay, 1 = 1 clock delay
  parameter INITIAL_INPUT_LEVEL = 0
) (
  input wire clk,
  input wire clk_en,
  input wire in,
  output reg edge_detected
);
  reg in_r0 = INITIAL_INPUT_LEVEL;
  reg in_r1 = INITIAL_INPUT_LEVEL;

  always @(posedge clk)
   if (!CLK_DLY)
      in_r0 <= in;
    else if (clk_en) begin
      in_r0 <= in;
      in_r1 <= in_r0;
    end

  always @ *
    if (EDGE_LEVEL)
      if (!CLK_DLY)
        edge_detected = in && !in_r0;
      else
        edge_detected = in_r0 && !in_r1;
    else
      if (!CLK_DLY)
        edge_detected = !in && in_r0;
      else
        edge_detected = !in_r0 && in_r1;
endmodule
