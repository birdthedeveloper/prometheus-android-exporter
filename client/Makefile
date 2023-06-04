# Makefile for protobuf generation for "remote write" prometheus protocol
#
# Input: 'proto' folder
# Output: 'protogen' package

.PHONY: protobuf

SRC_DIR_PROTO=app/src/main/java/com/birdthedeveloper/prometheus/android/prometheus/android/exporter/proto
DST_DIR_PROTO=app/src/main/java/com/birdthedeveloper/prometheus/android/prometheus/android/exporter/

protobuf:
	mkdir -p $(DST_DIR_PROTO)
	protoc  -I=$(SRC_DIR_PROTO) --kotlin_out=$(DST_DIR_PROTO) $(SRC_DIR_PROTO)/remote_write.proto

