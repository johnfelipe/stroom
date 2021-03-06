package stroom.data.store.impl.fs;

import stroom.data.store.impl.fs.shared.FsVolume;
import stroom.docref.SharedObject;
import stroom.util.shared.BaseResultList;

public interface DataVolumeService {
    BaseResultList<DataVolume> find(FindDataVolumeCriteria criteria);

    DataVolume findDataVolume(long dataId);

    DataVolume createStreamVolume(long dataId, FsVolume volume);

    interface DataVolume extends SharedObject {
        long getStreamId();

        String getVolumePath();

//        VolumeType getVolumeType();
//
//        int getNodeId();
//
//        int getRackId();
    }


//    interface StreamAndVolumes extends SharedObject {
//        Stream getMeta();
//
//        Set<Volume> getVolumes();
//    }
//
//    class StreamAndVolumesImpl implements StreamAndVolumes {
//        private final Stream stream;
//        private final Set<Volume> volumes;
//
//        StreamAndVolumesImpl(final Stream stream,
//                             final Set<Volume> volumes) {
//            this.stream = stream;
//            this.volumes = volumes;
//        }
//
//        public Stream getMeta() {
//            return stream;
//        }
//
//        public Set<Volume> getVolumes() {
//            return volumes;
//        }
//    }
}
