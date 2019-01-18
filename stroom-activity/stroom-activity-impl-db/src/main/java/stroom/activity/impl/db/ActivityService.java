/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.activity.impl.db;

import stroom.activity.api.Activity;
import stroom.activity.shared.FindActivityCriteria;
import stroom.entity.shared.BaseResultList;

public interface ActivityService {
    Activity create();

    Activity update(Activity activity);

    int delete(int id);

    Activity fetch(int id);

    BaseResultList<Activity> find(FindActivityCriteria criteria);
}
