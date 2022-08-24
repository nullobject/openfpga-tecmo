REVISION_NAME = ap_core
CORE_NAME = nullobject.tecmo

.PHONY: build clean copy program-pocket

build:
	bin/mill tecmo.run
	cd quartus; quartus_sh --flow compile $(REVISION_NAME)
	bin/reverse quartus/output_files/$(REVISION_NAME).rbf dist/Cores/$(CORE_NAME)/bitstream.rbf_r

copy:
	rsync -avh --progress dist/ /media/josh/2470-BED0 && umount /media/josh/2470-BED0

program-pocket:
	cd quartus; quartus_pgm -m jtag -c USB-Blaster -o "p;output_files/$(REVISION_NAME).sof@1"

clean:
	rm -rf dist/Cores/$(CORE_NAME)/out quartus/core/Tecmo.* quartus/db quartus/incremental_db quartus/output_files test_run_dir
