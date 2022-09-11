REVISION_NAME = ap_core
CORE_NAME = nullobject.tecmo
CORE_VERSION = $(shell jq ".core.metadata.version" -r dist/Cores/$(CORE_NAME)/core.json)

.PHONY: build clean copy mra program release

build:
	bin/mill tecmo.run
	cd quartus; quartus_sh --flow compile $(REVISION_NAME)
	bin/reverse quartus/output_files/$(REVISION_NAME).rbf dist/Cores/$(CORE_NAME)/bitstream.rbf_r

mra:
	cd mra; mra -z ~/mame/roms *.mra

copy:
	rsync -avh --progress --exclude \*.zip dist/ /media/josh/2470-BED0 && umount /media/josh/2470-BED0

program:
	cd quartus; quartus_pgm -m jtag -c USB-Blaster -o "p;output_files/$(REVISION_NAME).sof@1"

release:
	cd dist; zip -rv pocket-tecmo-$(CORE_VERSION).zip * -x \*.gitkeep \*.zip

clean:
	rm -rf dist/Cores/$(CORE_NAME)/out quartus/core/Tecmo.* quartus/db quartus/incremental_db quartus/output_files test_run_dir
