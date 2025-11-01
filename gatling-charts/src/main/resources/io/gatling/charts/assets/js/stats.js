function openStatisticsTableModal () {
  const statsTable = document.getElementById("StatsContainerId");
  const statsTableModal = document.getElementById("statistics_table_modal");
  const fullScreenButton = document.getElementById("statistics_full_screen");

  fullScreenButton.disabled = true;

  if (typeof statsTableModal.showModal === "function") {
    const statsTableModalContent = document.getElementById("statistics_table_modal_content");

    statsTableModalContent.innerHTML = "";
    statsTableModalContent.appendChild(statsTable);
    statsTableModal.showModal();

    statsTableModal.addEventListener("close", function () {
      const container = document.getElementById("statistics_table_container");

      container.appendChild(statsTable);
      fullScreenButton.disabled = false;
    });
  } else {
    const incompatibleBrowserVersionMessage = document.createElement("div");

    incompatibleBrowserVersionMessage.innerText = "Sorry, the <dialog> API is not supported by this browser.";
    statsTable.insertBefore(incompatibleBrowserVersionMessage, statsTable.children[0]);
  }
}

function closeStatisticsTableModal () {
  const statsTableModal = document.getElementById("statistics_table_modal");

  statsTableModal.close();
}
