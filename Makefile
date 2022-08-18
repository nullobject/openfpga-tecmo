CORE_NAME = ap_core

.PHONY: build clean program-pocket

build:
	bin/mill tecmo.run
	cd quartus; quartus_sh --flow compile $(CORE_NAME)
	bin/reverse quartus/output_files/$(CORE_NAME).rbf dist/bitstream.rbf_r

program-pocket:
	cd quartus; quartus_pgm -m jtag -c USB-Blaster -o "p;output_files/ap_core.sof@1"

clean:
	rm -rf dist/bitstream.rbf_r out quartus/core/Tecmo.* quartus/db quartus/incremental_db quartus/output_files test_run_dir
