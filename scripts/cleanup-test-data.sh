#!/usr/bin/env bash
# Deletes rows created by the Selenium journeys
# (every test item name starts with "Sel").
psql -w -h localhost -U pwos_user -d pwos_db -q -c \
  "DELETE FROM work_orders WHERE item_id IN
     (SELECT id FROM items WHERE name LIKE 'Sel%');
   DELETE FROM items WHERE name LIKE 'Sel%';" \
  && echo "Selenium test data removed" \
  || echo "WARNING: test data cleanup failed"
