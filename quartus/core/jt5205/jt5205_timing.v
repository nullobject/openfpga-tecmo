/*  This file is part of JT5205.
    JT5205 program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JT5205 program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JT5205.  If not, see <http://www.gnu.org/licenses/>.

    Author: Jose Tejada Gomez. Twitter: @topapate
    Version: 1.0
    Date: 30-10-2019 */

module jt5205_timing(
    input             clk,
    (* direct_enable *) input cen,
    input      [ 1:0] sel,        // s pin
    output            cen_lo,     // sample rate
    output            cenb_lo,    // sample rate (opposite phase)
    output            cen_mid,    // 2x sample rate
    output reg        vclk_o
);

parameter VCLK_CEN=1;

reg [6:0] cnt=0;
reg       pre=0, preb=0;
reg [6:0] lim;

always @(posedge clk) begin
    case(sel)
        0: lim <= 95;
        1: lim <= 63;
        2: lim <= 47;
        3: lim <=  1;
    endcase
end

always @(posedge clk) begin
    if(sel==3) begin
      cnt    <= 0;
      vclk_o <= 0;
    end
    if(cen) begin
        if(sel!=3) cnt <= cnt + 7'd1;

        pre    <= 0;
        preb   <= 0;
        if(cnt==lim) begin
            vclk_o <= 1;
            cnt    <= 0;
            pre    <= 1;
        end
        if(cnt==(lim>>1)) begin
          preb   <= 1;
          vclk_o <= 0;
        end
    end else if(VCLK_CEN) begin
        vclk_o <= 0;
    end
end

assign cen_lo  = pre &cen;
assign cenb_lo = preb&cen;
assign cen_mid = (pre|preb)&cen;

endmodule