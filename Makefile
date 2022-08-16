CORE_NAME = ap_core

.PHONY: build clean

build:
	bin/mill tecmo.run
	cd quartus; quartus_sh --flow compile $(CORE_NAME)
	bin/reverse quartus/output_files/$(CORE_NAME).rbf dist/bitstream.rbf_r

clean:
	rm -rf dist/bitstream.rbf_r out quartus/db quartus/incremental_db quartus/output_files
