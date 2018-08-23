import { createActions, handleActions, combineActions } from 'redux-actions';

const actionCreators = createActions({
  SELECTABLE_LISTING_MOUNTED: (listingId, items, allowMultiSelect) => ({
    listingId,
    items,
    allowMultiSelect,
  }),
  SELECTABLE_LISTING_UNMOUNTED: listingId => ({
    listingId,
    items: [],
  }),
  SELECTION_UP: listingId => ({ listingId, selectionChange: -1 }),
  SELECTION_DOWN: listingId => ({ listingId, selectionChange: +1 }),
  SELECTION_TOGGLED: (listingId, index, keyIsDown = {}) => ({ listingId, index, keyIsDown }),
});

const {
  selectableListingMounted,
  selectableListingUnmounted,
  selectionUp,
  selectionDown,
  selectionToggled,
} = actionCreators;

const defaultStatePerListing = {
  items: [],
  singleSelectedItemIndex: -1, // Used for simple item selection, by array index
  selectedItems: [],
  selectedItemIndexes: [],
};

// There will be an entry for each listing ID registered
const defaultState = {};

const reducer = handleActions(
  {
    [combineActions(selectableListingMounted, selectableListingUnmounted)]: (state, action) => {
      const {
        payload: { listingId, items, allowMultiSelect },
      } = action;

      return {
        ...state,
        [listingId]: {
          ...defaultStatePerListing,
          items,
          allowMultiSelect,
        },
      };
    },
    [combineActions(selectionUp, selectionDown)]: (state, action) => {
      const {
        payload: { listingId, selectionChange },
      } = action;
      const listingState = state[listingId];
      const { items, singleSelectedItemIndex } = listingState;

      let nextIndex = 0;
      if (singleSelectedItemIndex !== -1) {
        nextIndex = (items.length + (singleSelectedItemIndex + selectionChange)) % items.length;
      }

      const selectedItems = [items[nextIndex]];
      const selectedItemIndexes = [nextIndex];

      return {
        ...state,
        [listingId]: {
          ...listingState,
          singleSelectedItemIndex: nextIndex,
          selectedItems,
          selectedItemIndexes,
        },
      };
    },
    SELECTION_TOGGLED: (state, action) => {
      const {
        payload: { listingId, index, keyIsDown },
      } = action;
      const listingState = state[listingId];
      let {
        selectedItemIndexes, singleSelectedItemIndex, items, allowMultiSelect,
      } = listingState;

      const addToSelection = (arr, index) => {
        if (!arr.includes(index)) {
          arr.push(index);
        }
      };

      const isCurrentlySelected = selectedItemIndexes.includes(index);
      if (isCurrentlySelected) {
        if (keyIsDown.Control || keyIsDown.Meta) {
          selectedItemIndexes = selectedItemIndexes.filter((u, i) => i !== index);
        } else {
          selectedItemIndexes = [];
        }
      } else if (allowMultiSelect) {
        if (keyIsDown.Control || keyIsDown.Meta) {
          addToSelection(selectedItemIndexes, index);
        } else if (keyIsDown.Shift) {
          selectedItemIndexes = [];
          const phase = 0;
          items.forEach((n, nIndex) => {
            if (singleSelectedItemIndex < index) {
              for (let i = singleSelectedItemIndex; i <= index; i++) {
                addToSelection(selectedItemIndexes, i);
              }
            } else {
              for (let i = index; i <= singleSelectedItemIndex; i++) {
                addToSelection(selectedItemIndexes, i);
              }
            }
          });
        } else {
          selectedItemIndexes = [index];
        }
      } else {
        selectedItemIndexes = [index];
      }

      const selectedItems = selectedItemIndexes.map(i => items[i]);

      return {
        ...state,
        [listingId]: {
          ...listingState,
          selectedItems,
          selectedItemIndexes,
          singleSelectedItemIndex: index,
        },
      };
    },
  },
  defaultState,
);

export { actionCreators, reducer };
