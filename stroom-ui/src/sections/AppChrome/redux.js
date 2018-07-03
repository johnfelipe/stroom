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
import { createActions, handleActions } from 'redux-actions';

// Enumerate the tab types that can be opened
const TAB_TYPES = {
  DOC_REF: 0,
  EXPLORER_TREE: 1,
  TRACKER_DASHBOARD: 2,
  USER_ME: 3,
  AUTH_USERS: 4,
  AUTH_TOKENS: 5,
};

const actionCreators = createActions({
  TAB_OPENED: (type, tabId, data) => ({ type, tabId, data }),
  TAB_SELECTED: tabId => ({ tabId }),
  TAB_CLOSED: tabId => ({ tabId }),
});

const defaultState = {
  openTabs: [],
  tabSelectionStack: [],
};

const reducer = handleActions(
  {
    TAB_SELECTED: (state, action) => {
      const openTab = state.openTabs.find(t => t.tabId === action.payload.tabId);
      if (openTab) {
        return {
          ...state,
          tabSelectionStack: state.tabSelectionStack
            .filter(t => t !== action.payload.tabId)
            .concat([action.payload.tabId]),
        };
      }
      return state;
    },
    TAB_OPENED: (state, action) => {
      const tabId = action.payload.tabId || action.payload.type;
      if (state.openTabs.find(t => t.tabId === tabId)) {
        return {
          ...state,
          tabSelectionStack: state.tabSelectionStack.filter(t => t !== tabId).concat([tabId]),
        };
      }
      return {
        ...state,
        tabSelectionStack: state.tabSelectionStack.filter(t => t !== tabId).concat([tabId]),
        openTabs: state.openTabs.concat([
          {
            type: action.payload.type,
            tabId,
            data: action.payload.data,
          },
        ]),
      };
    },
    TAB_CLOSED: (state, action) => ({
      ...state,
      tabSelectionStack: state.tabSelectionStack.filter(t => t !== action.payload.tabId),
      openTabs: state.openTabs.filter(t => t.tabId !== action.payload.tabId),
    }),
  },
  defaultState,
);

export { TAB_TYPES, actionCreators, reducer };