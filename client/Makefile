# Makefile for protobuf generation for remote write prometheus protocol

.PHONY: protobuf

SRC_DIR_PROTO=app/src/main/java/com/birdthedeveloper/prometheus/android/prometheus/android/exporter/proto
DST_DIR_PROTO=../proto-gen/

protobuf:
	mkdir -p $(DST_DIR_PROTO)
	protoc  -I=$(SRC_DIR_PROTO) --kotlin_out=$(DST_DIR_PROTO) $(SRC_DIR_PROTO)/remote-write.proto

#TODO
