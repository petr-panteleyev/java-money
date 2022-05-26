/*
 Copyright (C) 2021, 2022 Petr Panteleyev

 This program is free software: you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation, either version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with this
 program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.panteleyev.money.bundles;

import java.util.ListResourceBundle;

import static org.panteleyev.money.bundles.Internationalization.*;

public class UiBundle extends ListResourceBundle {
    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                // Words
                {I18N_WORD_ACCOUNT, "Account"},
                {I18N_WORD_ACCOUNTS, "Accounts"},
                {I18N_WORD_ACTIVE, "Active"},
                {I18N_WORD_ADD, "Add"},
                {I18N_WORD_AFTER, "After"},
                {I18N_WORD_BALANCE, "Balance"},
                {I18N_WORD_BEFORE, "Before"},
                {I18N_WORD_CARD, "Card"},
                {I18N_WORD_CATEGORIES, "Categories"},
                {I18N_WORD_CATEGORY, "Category"},
                {I18N_WORD_CITY, "City"},
                {I18N_WORD_CLEAR, "Clear"},
                {I18N_WORD_CLOSE, "Close"},
                {I18N_WORD_COLORS, "Colors"},
                {I18N_WORD_COMMENT, "Comment"},
                {I18N_WORD_CONFIRMED, "Confirmed"},
                {I18N_WORD_CONNECTION, "Connection"},
                {I18N_WORD_CONTACT, "Contact"},
                {I18N_WORD_CONTACTS, "Contacts"},
                {I18N_WORD_CONTROLS, "Controls"},
                {I18N_WORD_COUNTERPARTY, "Counterparty"},
                {I18N_WORD_COUNTRY, "Country"},
                {I18N_WORD_CREDIT, "Credit"},
                {I18N_WORD_CURRENCIES, "Currencies"},
                {I18N_WORD_CURRENCY, "Currency"},
                {I18N_WORD_DATE, "Date"},
                {I18N_WORD_DAY, "Day"},
                {I18N_WORD_DEBIT, "Debit"},
                {I18N_WORD_DELETE, "Delete"},
                {I18N_WORD_DELTA, "Delta"},
                {I18N_WORD_DESCRIPTION, "Description"},
                {I18N_WORD_DETAILS, "Details"},
                {I18N_WORD_DIALOGS, "Dialogs"},
                {I18N_WORD_DOCUMENT, "Document"},
                {I18N_WORD_DOCUMENTS, "Documents"},
                {I18N_WORD_EDIT, "Edit"},
                {I18M_WORD_EXIT, "Exit"},
                {I18N_WORD_EXPENSES, "Expenses"},
                {I18N_WORD_EXPORT, "Export"},
                {I18N_WORD_FILE, "File"},
                {I18N_WORD_FONTS, "Fonts"},
                {I18N_WORD_GENERAL, "General"},
                {I18N_WORD_ICONS, "Icons"},
                {I18N_WORD_IMPORT, "Import"},
                {I18N_WORD_INCOMES, "Incomes"},
                {I18N_WORD_INTEREST, "Interest"},
                {I18N_WORD_INVOICE, "Invoice"},
                {I18N_WORD_LOGIN, "Login"},
                {I18N_WORD_MENU, "Menu"},
                {I18N_WORD_MOBILE, "Mobile"},
                {I18N_WORD_NAME, "Name"},
                {I18N_WORD_OPEN, "Open"},
                {I18N_WORD_OPTIONS, "Options"},
                {I18N_WORD_PASSWORD, "Password"},
                {I18N_WORD_PHONE, "Phone"},
                {I18N_WORD_PLACE, "Place"},
                {I18N_WORD_PORT, "Port"},
                {I18N_WORD_PROFILE, "Profile"},
                {I18N_WORD_PROFILES, "Profiles"},
                {I18N_WORD_RATE, "Rate"},
                {I18N_WORD_REPORT, "Report"},
                {I18N_WORD_REQUESTS, "Requests"},
                {I18N_WORD_RESET, "Reset"},
                {I18N_WORD_SAVE, "Save"},
                {I18N_WORD_SCHEMA, "Schema"},
                {I18N_WORD_SERVER, "Server"},
                {I18N_WORD_SIZE, "Size"},
                {I18N_WORD_STATEMENT, "Statement"},
                {I18N_WORD_STATEMENTS, "Statements"},
                {I18N_WORD_STREET, "Street"},
                {I18N_WORD_SUM, "Sum"},
                {I18N_WORD_SYMBOL, "Symbol"},
                {I18N_WORD_TABLES, "Tables"},
                {I18N_WORD_TEST, "Test"},
                {I18N_WORD_TEXT, "Text"},
                {I18N_WORD_TRANSACTION, "Transaction"},
                {I18N_WORD_TRANSACTIONS, "Transactions"},
                {I18N_WORD_TRANSFER, "Transfer"},
                {I18N_WORD_TYPE, "Type"},
                {I18N_WORD_UNCONFIRMED, "Unconfirmed"},
                {I18N_WORD_UNTIL, "Until"},
                {I18N_WORD_UPDATE, "Update"},
                {I18N_WORD_UPLOAD, "Upload"},
                {I18N_WORD_WAITING, "Waiting"},
                {I18N_WORD_ZIP, "ZIP"},
                // Menu
                {I18N_MENU_EDIT, "Edit"},
                {I18N_MENU_VIEW, "View"},
                {I18N_MENU_TOOLS, "Tools"},
                {I18N_MENU_WINDOW, "Window"},
                {I18N_MENU_HELP, "Help"},
                // Menu items
                {I18N_MENU_ITEM_ABOUT, "About"},
                {I18N_MENU_ITEM_ACTIVATE, "Activate"},
                {I18N_MENU_ITEM_CHECK, "Check"},
                {I18N_MENU_ITEM_COPY_NAME, "Copy Name"},
                {I18N_MENU_ITEM_CURRENT_MONTH, "Current Month"},
                {I18N_MENU_ITEM_DEACTIVATE, "Deactivate"},
                {I18N_MENU_ITEM_NEXT_MONTH, "Next Month"},
                {I18N_MENU_ITEM_PREVIOUS_MONTH, "Previous Month"},
                {I18N_MENU_ITEM_SEARCH, "Search"},
                {I18N_MENU_ITEM_UNCHECK, "Uncheck"},
                {I18N_MENU_ITEM_UPLOAD, "Upload"},
                // Misc
                {I18N_MISC_ACCOUNT_NUMBER, "Account Number"},
                {I18N_MISC_ACCOUNTS_CASH_CARDS, "Accounts, cash, cards"},
                {I18N_MISC_ALL_ACCOUNTS, "All Accounts"},
                {I18N_MISC_ALL_CATEGORIES, "All Categories"},
                {I18N_MISC_ALL_FILES, "All Files"},
                {I18N_MISC_ALL_TYPES, "All Types"},
                {I18N_MISC_ARE_YOU_SURE, "Are you sure?"},
                {I18N_MISC_AUTOCOMPLETE_PREFIX_LENGTH, "Autocomplete Prefix Length"},
                {I18N_MISC_CARD_NUMBER, "Card Number"},
                {I18N_MISC_CARD_TYPE, "Card Type"},
                {I18N_MISC_CLOSING_DATE, "Closing Date"},
                {I18N_MISC_CONNECT_AT_STARTUP, "Connect at Startup"},
                {I18N_MISC_CREDITED_ACCOUNT, "Credited Account"},
                {I18N_MISC_DATE_BY_STATEMENT, "Date by Statement"},
                {I18N_MISC_DATE_PICKER_TOOLTIP, "%s - next day\n%s - previous day\n%s - next month\n%s - previous " +
                        "month\n%s - today"},
                {I18N_MISC_DAYS_BEFORE_CLOSING, "Days Before Account Closing"},
                {I18N_MISC_DEBITED_ACCOUNT, "Debited Account"},
                {I18N_MISC_DEFAULT_CURRENCY, "Default Currency"},
                {I18N_MISC_DEFAULT_PROFILE, "Default Profile"},
                {I18N_WORD_ENTITY_NAME, "Name"},
                {I18N_MISC_EXECUTION_DATE_SHORT, "Exec. Date"},
                {I18N_MISC_FULL_DUMP_IMPORT_CHECK, "Yes, I'm sure!"},
                {I18N_MISC_FULL_DUMP_IMPORT_WARNING, "Warning!\nImporting full dump will complete erase all\nexisting" +
                        " records."},
                {I18N_MISC_IGNORE_EXECUTION_DATE, "Ignore Execution Date"},
                {I18N_MISC_IMPORT_FILE_NAME, "Import file name"},
                {I18M_MISC_INCOMES_AND_EXPENSES, "Incomes and Expenses"},
                {I18N_MISC_INCOMPATIBLE_SCHEMA, "Database is incompatible with the application, exiting."},
                {I18N_MISC_INITIAL_BALANCE, "Initial Balance"},
                {I18N_MISC_NOT_FOUND, "Not Found"},
                {I18N_MISC_PARTIAL_IMPORT, "Partial import"},
                {I18N_MISC_PROFILE_NAME, "Profile Name"},
                {I18N_MISC_RECALCULATE_BALANCE, "Recalculate Balance"},
                {I18N_MISC_RESET_FILTER, "Reset Filter"},
                {I18N_MISC_SCHEMA_RESET, "Database Reset"},
                {I18M_MISC_SCHEMA_RESET_HEADER, "Attention!"},
                {I18N_MISC_SCHEMA_RESET_TEXT, "All data in this database will be lost. " +
                        "Please make sure there is a backup file. Continue?"},
                {I18N_MISC_SCHEMA_UPDATE, "Database Update"},
                {I18N_MISC_SCHEMA_UPDATE_TEXT, "Database requires compatible update. Continue?"},
                {I18N_MISC_SHOW_DEACTIVATED_ACCOUNTS, "Show deactivated accounts"},
                {I18N_MISC_SHOW_THOUSAND_SEPARATOR, "Show thousand separator"},
                {I18N_MISC_STATEMENT_BALANCE, "Statement Balance"},
                {I18N_MISC_TRANSACTION_DETAILS, "Transaction Details"},
                {I18N_MISC_UNCHECKED_ONLY, "Unchecked only"},
                {I18N_MISC_UPLOAD_DIPLICATE, ": duplicate"},
                {I18N_MISC_UPLOAD_FILES_SKIPPED, "Files skipped:"},
                {I18N_MISC_UPLOAD_TOO_BIG, ": too big"},
                {I18N_MISC_XML_FILES, "XML Files"},
                {I18N_MISC_ZIP_FILES, "ZIP Files"},
                {I18N_CREATE_DESKTOP_ENTRY, "Create Desktop Entry (Linux)"},
                //
                {"button.Cancel", "Cancel"},
        };
    }
}
