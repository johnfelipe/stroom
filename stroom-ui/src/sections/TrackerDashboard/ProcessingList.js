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

import React from 'react';
import PropTypes from 'prop-types';

import { connect } from 'react-redux';
import { compose, lifecycle, withProps, withHandlers } from 'recompose';
import Mousetrap from 'mousetrap';

import { Table, Label, Progress } from 'semantic-ui-react';

import { actionCreators, Directions, SortByOptions } from './redux';

import { fetchTrackers, TrackerSelection } from './streamTasksResourceClient';

// const { expressionChanged } = expressionActionCreators;
const {
  updateSort,
  updateTrackerSelection,
  moveSelection,
  resetPaging,
  updateSearchCriteria,
  changePage,
  pageRight,
  pageLeft,
} = actionCreators;

const enhance = compose(
  connect(
    ({
      trackerDashboard: {
        // isLoading,
        trackers,
        // showCompleted,
        sortBy,
        sortDirection,
        selectedTrackerId,
        // searchCriteria,
        // pageSize,
        // pageOffset,
        // totalTrackers,
        // numberOfPages,
      },
    }) => ({
      // isLoading,
      trackers,
      // showCompleted,
      sortBy,
      sortDirection,
      selectedTrackerId,
      // searchCriteria,
      // pageSize,
      // pageOffset,
      // totalTrackers,
      // numberOfPages,
    }),
    {
      fetchTrackers,
      resetPaging,
      updateSort,
      updateTrackerSelection,
      // expressionChanged,
      moveSelection,
      updateSearchCriteria,
      changePage,
      pageRight,
      pageLeft,
    },
  ),
  withHandlers({
    onMoveSelection: ({ moveSelection }) => (direction) => {
      moveSelection(direction);
    },

    onHandlePageChange: ({ changePage, fetchTrackers }) => (data) => {
      if (data.activePage < data.totalPages) {
        changePage(data.activePage - 1);
        fetchTrackers();
      }
    },
    onHandlePageRight: ({ pageRight, fetchTrackers }) => () => {
      pageRight();
      fetchTrackers(TrackerSelection.first);
    },
    onHandlePageLeft: ({ pageLeft, fetchTrackers }) => () => {
      pageLeft();
      fetchTrackers(TrackerSelection.first);
    },
    onHandleSort: ({ updateSort, fetchTrackers }) => (
      newSortBy,
      currentSortBy,
      currentDirection,
    ) => {
      if (currentSortBy === newSortBy) {
        if (currentDirection === Directions.ascending) {
          updateSort(newSortBy, Directions.descending);
          fetchTrackers();
        }
        updateSort(newSortBy, Directions.ascending);
        fetchTrackers();
      }
      updateSort(newSortBy, Directions.ascending);
      fetchTrackers();
    },
  }),
  withProps(({ trackers, selectedTrackerId }) => ({
    selectedTracker: trackers.find(tracker => tracker.filterId === selectedTrackerId),
  })),
  lifecycle({
    componentDidMount() {
      const {
        fetchTrackers,
        resetPaging,
        onMoveSelection,
        onHandlePageRight,
        onHandlePageLeft,
        onHandleTrackerSelection,
        onHandleSearch,
      } = this.props;

      fetchTrackers();

      Mousetrap.bind('up', () => onMoveSelection('up'));
      Mousetrap.bind('down', () => onMoveSelection('down'));
      Mousetrap.bind('right', () => onHandlePageRight());
      Mousetrap.bind('left', () => onHandlePageLeft());
      Mousetrap.bind('esc', () => onHandleTrackerSelection(undefined));
      Mousetrap.bind('ctrl+shift+f', () => this.searchInputRef.focus());
      Mousetrap.bind('enter', () => onHandleSearch());
      Mousetrap.bind('return', () => onHandleSearch());

      // This component monitors window size. For every change it will fetch the
      // trackers. The fetch trackers function will only fetch trackers that fit
      // in the viewport, which means the view will update to fit.
      window.addEventListener('resize', (event) => {
        // Resizing the window is another time when paging gets reset.
        resetPaging();
        fetchTrackers();
      });
    },
  }),
);

const ProcessingList = ({
  sortBy,
  sortDirection,
  trackers,
  selectedTrackerId,
  onHandleSort,
  onSelection,
}) => (
  <Table selectable sortable basic="very" className="tracker-table" columns={15}>
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell
          sorted={sortBy === SortByOptions.pipelineUuid ? sortDirection : null}
          onClick={() => onHandleSort(SortByOptions.pipeline, sortBy, sortDirection)}
        >
          Pipeline name
        </Table.HeaderCell>
        <Table.HeaderCell
          sorted={sortBy === SortByOptions.priority ? sortDirection : null}
          onClick={() => onHandleSort(SortByOptions.priority, sortBy, sortDirection)}
        >
          Priority
        </Table.HeaderCell>
        <Table.HeaderCell
          sorted={sortBy === SortByOptions.progress ? sortDirection : null}
          onClick={() => onHandleSort(SortByOptions.progress, sortBy, sortDirection)}
        >
          Progress
        </Table.HeaderCell>
      </Table.Row>
    </Table.Header>

    <Table.Body>
      {trackers.map(({
          pipelineName,
          priority,
          trackerPercent,
          filterId,
          createdOn,
          createUser,
          updateUser,
          updatedOn,
          enabled,
          status,
          lastPollAge,
          taskCount,
          trackerMs,
          streamCount,
          eventCount,
        }) => (
          <Table.Row
            key={filterId}
            className="tracker-row"
            onClick={() => onSelection(filterId, trackers)}
            active={selectedTrackerId === filterId}
          >
            <Table.Cell className="name-column" textAlign="left" width={7}>
              TODO: backend broken, awaiting re-write
            </Table.Cell>
            <Table.Cell className="priority-column" textAlign="center" width={1}>
              <Label circular color="green">
                {priority}
              </Label>
            </Table.Cell>
            <Table.Cell className="progress-column" width={7}>
              <Progress indicating percent={trackerPercent} />
            </Table.Cell>
          </Table.Row>
        ))}
    </Table.Body>
  </Table>
);

ProcessingList.propTypes = {
  onSelection: PropTypes.func.isRequired,
};

export default enhance(ProcessingList);
