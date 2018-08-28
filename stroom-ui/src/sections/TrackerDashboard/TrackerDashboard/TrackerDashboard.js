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

import PanelGroup from 'react-panelgroup';

import ReactTable from 'react-table';
import 'react-table/react-table.css';

import {
  Header,
  Icon,
  Label,
  Table,
  Progress,
  Button,
  Input,
  Menu,
  Pagination,
  Grid,
} from 'semantic-ui-react';

import { actionCreators, Directions, SortByOptions } from '../redux';
import { actionCreators as expressionActionCreators } from 'components/ExpressionBuilder';
import { fetchTrackers, TrackerSelection } from '../streamTasksResourceClient';
import TrackerDetails from '../TrackerDetails/TrackerDetails';

import ProcessingList from '../ProcessingList';

const { expressionChanged } = expressionActionCreators;
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
        isLoading,
        trackers,
        showCompleted,
        sortBy,
        sortDirection,
        selectedTrackerId,
        searchCriteria,
        pageSize,
        pageOffset,
        totalTrackers,
        numberOfPages,
      },
    }) => ({
      isLoading,
      trackers,
      showCompleted,
      sortBy,
      sortDirection,
      selectedTrackerId,
      searchCriteria,
      pageSize,
      pageOffset,
      totalTrackers,
      numberOfPages,
    }),
    {
      fetchTrackers,
      resetPaging,
      updateSort,
      updateTrackerSelection,
      expressionChanged,
      moveSelection,
      updateSearchCriteria,
      changePage,
      pageRight,
      pageLeft,
    },
  ),
  withHandlers({
    onHandleTrackerSelection: ({ updateTrackerSelection, expressionChanged }) => (
      filterId,
      trackers,
    ) => {
      updateTrackerSelection(filterId);

      let expression;
      if (filterId !== undefined) {
        const tracker = trackers.find(t => t.filterId === filterId);
        if (tracker && tracker.filter) {
          expression = tracker.filter.expression;
        }
      }

      expressionChanged('trackerDetailsExpression', expression);
    },
    onMoveSelection: ({ moveSelection }) => (direction) => {
      moveSelection(direction);
    },
    onHandleSearchChange: ({ resetPaging, updateSearchCriteria, fetchTrackers }) => (data) => {
      resetPaging();
      updateSearchCriteria(data.value);
      // This line enables search as you type. Whether we want it or not depends on performance
      fetchTrackers();
    },
    onHandleSearch: ({ fetchTrackers }) => (event) => {
      if (event === undefined || event.key === 'Enter') {
        fetchTrackers();
      }
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
  withProps(({ selectedTracker }) => ({
    showDetails: selectedTracker !== undefined,
  })),
  withProps(({ showDetails }) => ({
    // 370 is the minimum height because it lets all the tracker details be displayed
    detailsPanelMinimumHeight: showDetails ? 200 : 0,
  })),
  withProps(({ detailsPanelMinimumHeight, showDetails }) => ({
    panelSizes: [
      {},
      {
        resize: 'dynamic',
        minSize: detailsPanelMinimumHeight,
        size: showDetails ? detailsPanelMinimumHeight : 0,
      },
    ],
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

const TrackerDashboard = ({
  trackers,
  sortBy,
  sortDirection,
  selectedTrackerId,
  searchCriteria,
  pageOffset,
  numberOfPages,
  updateSort,
  fetchTrackers,
  updateTrackerSelection,
  expressionChanged,
  selectedTracker,
  showDetails,
  detailsPanelMinimumHeight,
  panelSizes,
  onHandleSort,
  onHandleTrackerSelection,
  onMoveSelection,
  onHandleSearchChange,
  onHandleSearch,
  onHandlePageChange,
  onHandlePageRight,
  onHandlePageLeft,
}) => (
  <React.Fragment>
    <Grid className="content-tabs__grid">
      <Grid.Column width={12}>
        <Header as="h3">
          <Icon name="play" />
          <Header.Content className="header">Processing</Header.Content>
        </Header>
      </Grid.Column>
    </Grid>
    <div className="tracker-container">
      <div className="tracker">
        <Menu attached="top">
          <Menu.Menu position="left" className="search-container">
            <Input
              fluid
              placeholder="Search..."
              value={searchCriteria}
              onChange={(event, data) => onHandleSearchChange(data)}
              onKeyPress={(event, data) => onHandleSearch(event, data)}
              action={<Button className="icon-button" onClick={() => onHandleSearch()} />}
              // We can set the ref to 'this', which means we can call this.searchInputRef.focus() elsewhere.
              ref={input => (this.searchInputRef = input)}
            />
          </Menu.Menu>
        </Menu>
        <PanelGroup direction="column">
          <div className="processing__table__container">
            <div
              id="table-container"
              className={`table-container${showDetails ? ' showing-details' : ''}`}
            >
              <ProcessingList onSelection={(filterId, trackers) => onHandleTrackerSelection(filterId, trackers)}/>
              <div className="pagination-container">
                <Pagination
                  activePage={pageOffset + 1}
                  totalPages={numberOfPages || 1}
                  firstItem={null}
                  lastItem={null}
                  size="tiny"
                  onPageChange={(event, data) => onHandlePageChange(data)}
                />
              </div>
            </div>
          </div>
          <TrackerDetails />
        </PanelGroup>
      </div>
    </div>
  </React.Fragment>
);

TrackerDashboard.contextTypes = {
  store: PropTypes.object.isRequired,
};

export default enhance(TrackerDashboard);
